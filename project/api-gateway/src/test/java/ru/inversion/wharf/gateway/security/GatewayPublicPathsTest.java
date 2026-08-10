package ru.inversion.wharf.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
class GatewayPublicPathsTest {

    @Autowired
    private WebTestClient client;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/.well-known/jwks.json",
            "/api/v1/catalog/.well-known/jwks.json",
    })
    void jwksEndpointsAreReachableWithoutToken(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .describedAs("JWKS %s обязан быть публичным на шлюзе: агент забирает "
                                + "trust anchor'ы до всякой авторизации", path)
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/enroll",
    })
    void authEntryPointsAreReachableWithoutToken(String path) {
        client.post().uri(path)
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/catalog/products",
            "/api/v1/releases",
            "/api/v1/entitlements",
            "/api/v1/intents",
            "/api/v1/telemetry/audit",
    })
    void domainApiRequiresToken(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void fallbackIsReachableWithoutToken() {
        client.get().uri("/fallback/catalog-service")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
