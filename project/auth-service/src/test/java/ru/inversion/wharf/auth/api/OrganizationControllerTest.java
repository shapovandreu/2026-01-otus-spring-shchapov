package ru.inversion.wharf.auth.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.Organization;
import ru.inversion.wharf.auth.security.JwtKeysConfig;
import ru.inversion.wharf.auth.security.SecurityConfig;
import ru.inversion.wharf.auth.service.OrganizationService;
import ru.inversion.wharf.common.audit.AuditPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = OrganizationController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class OrganizationControllerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @MockitoBean
    private OrganizationService organizations;

    @MockitoBean
    private AuditPublisher audit;

    @Autowired
    private WebTestClient client;

    @Test
    void releaseManagerReadsOrganizations() {
        when(organizations.list()).thenReturn(Flux.just(new Organization(ORG_ID, "acme", Instant.now())));

        client.mutateWith(mockJwt().authorities(role("ROLE_RM")))
                .get().uri("/api/v1/auth/orgs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(ORG_ID.toString())
                .jsonPath("$[0].name").isEqualTo("acme");
    }

    @Test
    void agentCannotReadOrganizations() {
        client.mutateWith(mockJwt().authorities(role("ROLE_AGENT")))
                .get().uri("/api/v1/auth/orgs")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void anonymousCannotReadOrganizations() {
        client.get().uri("/api/v1/auth/orgs")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adminCreatesOrganization() {
        when(audit.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(organizations.create(any())).thenReturn(
                Mono.just(new Organization(ORG_ID, "globex", Instant.now())));

        create(role("ROLE_ADMIN"))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("globex");
    }

    @Test
    void licenseManagerCannotCreateOrganization() {
        create(role("ROLE_LM")).expectStatus().isForbidden();
    }

    @Test
    void releaseManagerCannotCreateOrganization() {
        create(role("ROLE_RM")).expectStatus().isForbidden();
    }

    @Test
    void rejectsBlankName() {
        client.mutateWith(mockJwt().authorities(role("ROLE_ADMIN")))
                .post().uri("/api/v1/auth/orgs")
                .bodyValue(Map.of("name", "  "))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private WebTestClient.ResponseSpec create(SimpleGrantedAuthority authority) {
        return client.mutateWith(mockJwt().authorities(authority))
                .post().uri("/api/v1/auth/orgs")
                .bodyValue(Map.of("name", "globex"))
                .exchange();
    }

    private static SimpleGrantedAuthority role(String role) {
        return new SimpleGrantedAuthority(role);
    }
}
