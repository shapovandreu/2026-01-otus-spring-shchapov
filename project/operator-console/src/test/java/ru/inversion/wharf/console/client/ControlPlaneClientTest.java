package ru.inversion.wharf.console.client;

import java.time.Duration;
import java.util.UUID;

import ru.inversion.wharf.console.config.ConsoleProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneClientTest {

    private static final String TOKEN = "operator-jwt";

    private MockWebServer controlPlane;
    private ControlPlaneClient client;

    @BeforeEach
    void startControlPlane() throws Exception {
        controlPlane = new MockWebServer();
        controlPlane.start();

        ConsoleProperties properties = new ConsoleProperties(
                controlPlane.url("/").toString(),
                Duration.ofSeconds(30),
                new ConsoleProperties.Session("iw_operator", false));
        WebClient web = WebClient.builder().baseUrl(properties.gatewayUrl()).build();
        client = new ControlPlaneClient(web, properties);
    }

    @AfterEach
    void stopControlPlane() throws Exception {
        controlPlane.shutdown();
    }

    @Test
    void passesOperatorTokenToControlPlane() throws Exception {
        controlPlane.enqueue(json(HttpStatus.OK, """
                [{"id":"11111111-1111-1111-1111-111111111111","name":"acme","createdAt":"2026-07-21T10:00:00Z"}]
                """));

        StepVerifier.create(client.organizations(TOKEN))
                .assertNext(org -> assertThat(org.name()).isEqualTo("acme"))
                .verifyComplete();

        RecordedRequest request = controlPlane.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/auth/orgs");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TOKEN);
    }

    @Test
    void translatesProblemDetailIntoDomainError() {
        controlPlane.enqueue(json(HttpStatus.CONFLICT, """
                {"status":409,"title":"release-immutable","detail":"Релиз уже опубликован",
                 "code":"RELEASE_IMMUTABLE"}
                """));

        StepVerifier.create(client.publishRelease(TOKEN, UUID.randomUUID()))
                .verifyErrorSatisfies(error -> {
                    ControlPlaneException failure = (ControlPlaneException) error;
                    assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(failure.code()).isEqualTo("RELEASE_IMMUTABLE");
                    assertThat(failure.getMessage()).isEqualTo("Релиз уже опубликован");
                });
    }

    @Test
    void treatsBodilessUnauthorizedAsExpiredSession() {
        controlPlane.enqueue(new MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value()));

        StepVerifier.create(client.products(TOKEN))
                .verifyErrorSatisfies(error ->
                        assertThat(((ControlPlaneException) error).isUnauthorized()).isTrue());
    }

    @Test
    void reportsUnreachableControlPlaneAsUnavailable() throws Exception {
        controlPlane.shutdown();

        StepVerifier.create(client.organizations(TOKEN))
                .verifyErrorSatisfies(error -> {
                    ControlPlaneException failure = (ControlPlaneException) error;
                    assertThat(failure.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    private static MockResponse json(HttpStatus status, String body) {
        return new MockResponse()
                .setResponseCode(status.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}
