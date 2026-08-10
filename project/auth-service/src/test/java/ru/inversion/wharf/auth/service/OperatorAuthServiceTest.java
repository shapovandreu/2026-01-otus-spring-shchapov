package ru.inversion.wharf.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.OperatorUserRepository;
import ru.inversion.wharf.common.api.Roles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorAuthServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock
    private OperatorUserRepository users;
    @Mock
    private JwtIssuer jwtIssuer;

    @Test
    void loginWithValidPasswordIssuesTokenWithRoles() {
        when(users.findByUsername("lmanager")).thenReturn(Mono.just(operator("lmanager", "secret", "LM")));
        JwtIssuer.IssuedToken issued = new JwtIssuer.IssuedToken("jwt", Instant.now().plusSeconds(60), 60);
        when(jwtIssuer.forOperator("lmanager", List.of("LM"))).thenReturn(issued);

        StepVerifier.create(service().login("lmanager", "secret"))
                .expectNext(issued)
                .verifyComplete();
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(users.findByUsername("lmanager")).thenReturn(Mono.just(operator("lmanager", "secret", "LM")));

        StepVerifier.create(service().login("lmanager", "wrong"))
                .expectError(AuthExceptions.InvalidCredentials.class)
                .verify();
    }

    @Test
    void loginWithUnknownUserFails() {
        when(users.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(service().login("ghost", "secret"))
                .expectError(AuthExceptions.InvalidCredentials.class)
                .verify();
    }

    @Test
    void orgUserGetsTokenBoundToOrganization() {
        UUID orgId = UUID.randomUUID();
        when(users.findByUsername("acme-admin")).thenReturn(Mono.just(
                orgUser("acme-admin", "secret", orgId)));
        JwtIssuer.IssuedToken issued = new JwtIssuer.IssuedToken("jwt", Instant.now().plusSeconds(60), 60);
        when(jwtIssuer.forOrgUser("acme-admin", List.of("ORG_ADMIN"), orgId)).thenReturn(issued);

        StepVerifier.create(service().login("acme-admin", "secret"))
                .expectNext(issued)
                .verifyComplete();

        verify(jwtIssuer, never()).forOperator(anyString(), any());
    }

    private OperatorAuthService service() {
        return new OperatorAuthService(users, ENCODER, jwtIssuer);
    }

    private static OperatorUser operator(String username, String password, String roles) {
        return new OperatorUser(UUID.randomUUID(), username, ENCODER.encode(password), roles, null, Instant.now());
    }

    private static OperatorUser orgUser(String username, String password, UUID orgId) {
        return new OperatorUser(UUID.randomUUID(), username, ENCODER.encode(password),
                Roles.ORG_ADMIN, orgId, Instant.now());
    }
}
