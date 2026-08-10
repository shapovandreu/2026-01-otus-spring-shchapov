package ru.inversion.wharf.auth.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.auth.security.JwtKeysConfig;
import ru.inversion.wharf.auth.security.SecurityConfig;
import ru.inversion.wharf.auth.service.EnrollmentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = EnrollmentTokenController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class EnrollmentTokenControllerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @MockitoBean
    private EnrollmentService enrollment;

    @MockitoBean
    private AuditPublisher audit;

    @Autowired
    private WebTestClient client;

    @Test
    void licenseManagerIssuesToken() {
        when(audit.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(enrollment.issue(eq(ORG_ID), any())).thenReturn(Mono.just(
                new EnrollmentService.IssuedEnrollmentToken(
                        UUID.randomUUID(), ORG_ID, "one-time-secret", Instant.now().plusSeconds(3600))));

        post(role("ROLE_LM"))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.token").isEqualTo("one-time-secret")
                .jsonPath("$.orgId").isEqualTo(ORG_ID.toString());
    }

    @Test
    void agentCannotIssueToken() {
        post(role("ROLE_AGENT")).expectStatus().isForbidden();
    }

    @Test
    void releaseManagerCannotIssueToken() {
        post(role("ROLE_RM")).expectStatus().isForbidden();
    }

    @Test
    void anonymousCannotIssueToken() {
        client.post().uri("/api/v1/auth/enrollment-tokens")
                .bodyValue(Map.of("orgId", ORG_ID.toString()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WebTestClient.ResponseSpec post(SimpleGrantedAuthority authority) {
        return client.mutateWith(mockJwt().authorities(authority))
                .post().uri("/api/v1/auth/enrollment-tokens")
                .bodyValue(Map.of("orgId", ORG_ID.toString()))
                .exchange();
    }

    private static SimpleGrantedAuthority role(String role) {
        return new SimpleGrantedAuthority(role);
    }
}
