package ru.inversion.wharf.gateway.fallback;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FallbackController()).build();

    @Test
    void returnsRetryableProblemDetailWhenServiceIsDown() {
        client.get().uri("/fallback/catalog-service")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.detail").value(containsString("catalog-service"));
    }
}
