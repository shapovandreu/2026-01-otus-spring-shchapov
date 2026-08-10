package ru.inversion.wharf.auth.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;
import ru.inversion.wharf.auth.security.JwtKeysConfig;
import ru.inversion.wharf.auth.security.SecurityConfig;
import ru.inversion.wharf.auth.service.OrgUserService;
import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.common.audit.AuditPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = OrgAdminController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class OrgAdminControllerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FOREIGN_ORG = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @MockitoBean
    private OrgUserService orgUsers;

    @MockitoBean
    private AuditPublisher audit;

    @Autowired
    private WebTestClient client;

    @Test
    void agentCreatesAdminOfItsOwnOrganization() {
        when(audit.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(orgUsers.bootstrapAdmin(eq(ORG_ID), eq("acme-admin"), any())).thenReturn(Mono.just(saved(ORG_ID)));

        post(agentOf(ORG_ID))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("acme-admin")
                .jsonPath("$.orgId").isEqualTo(ORG_ID.toString());
    }

    @Test
    void organizationComesFromTokenNotFromRequest() {
        when(audit.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(orgUsers.bootstrapAdmin(eq(FOREIGN_ORG), any(), any())).thenReturn(Mono.just(saved(FOREIGN_ORG)));

        post(agentOf(FOREIGN_ORG)).expectStatus().isOk();

        verify(orgUsers, never()).bootstrapAdmin(eq(ORG_ID), any(), any());
    }

    @Test
    void vendorOperatorCannotUseAgentPath() {
        post(mockJwt().authorities(role("ROLE_ADMIN"))).expectStatus().isForbidden();

        verify(orgUsers, never()).bootstrapAdmin(any(), any(), any());
    }

    @Test
    void orgAdminCannotCreateAnotherAdmin() {
        post(mockJwt().authorities(role("ROLE_ORG_ADMIN"))).expectStatus().isForbidden();
    }

    @Test
    void anonymousCannotCreateAdmin() {
        client.post().uri("/api/v1/auth/org-admin")
                .bodyValue(body())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WebTestClient.ResponseSpec post(
            org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator jwt) {
        return client.mutateWith(jwt)
                .post().uri("/api/v1/auth/org-admin")
                .bodyValue(body())
                .exchange();
    }

    private static OperatorUser saved(UUID orgId) {
        return new OperatorUser(UUID.randomUUID(), "acme-admin", "hash", Roles.ORG_ADMIN, orgId, Instant.now());
    }

    private static Map<String, String> body() {
        return Map.of("username", "acme-admin", "password", "s3cret-pass");
    }

    private static org.springframework.security.test.web.reactive.server
            .SecurityMockServerConfigurers.JwtMutator agentOf(UUID orgId) {
        return mockJwt()
                .jwt(jwt -> jwt.claim(Roles.CLAIM_ORG, orgId.toString()))
                .authorities(role("ROLE_" + Roles.AGENT));
    }

    private static SimpleGrantedAuthority role(String role) {
        return new SimpleGrantedAuthority(role);
    }
}
