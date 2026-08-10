package ru.inversion.wharf.auth.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;
import ru.inversion.wharf.auth.domain.Organization;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.OperatorUserRepository;
import ru.inversion.wharf.common.api.Roles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgUserServiceTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID FOREIGN_ORG = UUID.randomUUID();

    @Mock
    private OperatorUserRepository users;
    @Mock
    private OrganizationService organizations;

    @SuppressWarnings("deprecation")
    private OrgUserService service() {
        return new OrgUserService(users, organizations, NoOpPasswordEncoder.getInstance());
    }

    @Test
    void bootstrapCreatesAdminWhenAbsent() {
        when(organizations.byId(ORG)).thenReturn(Mono.just(new Organization(ORG, "acme", Instant.now())));
        when(users.findByUsername("acme-admin")).thenReturn(Mono.empty());
        when(users.save(any(OperatorUser.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(service().bootstrapAdmin(ORG, "acme-admin", "s3cret-pass"))
                .assertNext(created -> {
                    assertThat(created.username()).isEqualTo("acme-admin");
                    assertThat(created.roles()).isEqualTo(Roles.ORG_ADMIN);
                    assertThat(created.orgId()).isEqualTo(ORG);
                })
                .verifyComplete();
    }

    @Test
    void bootstrapResetsPasswordOfOwnOrgAdmin() {
        UUID userId = UUID.randomUUID();
        OperatorUser existing = new OperatorUser(userId, "acme-admin", "old-hash",
                Roles.ORG_ADMIN, ORG, Instant.now());
        when(organizations.byId(ORG)).thenReturn(Mono.just(new Organization(ORG, "acme", Instant.now())));
        when(users.findByUsername("acme-admin")).thenReturn(Mono.just(existing));
        when(users.save(any(OperatorUser.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(service().bootstrapAdmin(ORG, "acme-admin", "new-password"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<OperatorUser> saved = ArgumentCaptor.forClass(OperatorUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(userId);
        assertThat(saved.getValue().passwordHash()).isEqualTo("new-password");
    }

    @Test
    void bootstrapRefusesToTouchUserOfAnotherOrganization() {
        OperatorUser foreign = new OperatorUser(UUID.randomUUID(), "acme-admin", "hash",
                Roles.ORG_ADMIN, FOREIGN_ORG, Instant.now());
        when(organizations.byId(ORG)).thenReturn(Mono.just(new Organization(ORG, "acme", Instant.now())));
        when(users.findByUsername("acme-admin")).thenReturn(Mono.just(foreign));

        StepVerifier.create(service().bootstrapAdmin(ORG, "acme-admin", "new-password"))
                .expectError(AuthExceptions.UserExists.class)
                .verify();

        verify(users, never()).save(any());
    }

    @Test
    void bootstrapRefusesToTouchVendorOperator() {
        OperatorUser operator = OperatorUser.create("acme-admin", "hash", "ADMIN,RM,LM", Instant.now());
        when(organizations.byId(ORG)).thenReturn(Mono.just(new Organization(ORG, "acme", Instant.now())));
        when(users.findByUsername("acme-admin")).thenReturn(Mono.just(operator));

        StepVerifier.create(service().bootstrapAdmin(ORG, "acme-admin", "new-password"))
                .expectError(AuthExceptions.UserExists.class)
                .verify();

        verify(users, never()).save(any());
    }
}
