package ru.inversion.wharf.client.client;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import ru.inversion.wharf.client.client.ClientViews.AvailableProductView;
import ru.inversion.wharf.client.client.ClientViews.InstallationStatus;
import ru.inversion.wharf.client.client.ClientViews.IntentView;
import ru.inversion.wharf.client.client.ClientViews.TelemetryView;
import ru.inversion.wharf.client.client.ClientViews.TokenResponse;
import ru.inversion.wharf.client.config.ClientConsoleProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ControlPlaneClient {

    private static final String AUTH = "/api/v1/auth";
    private static final String AVAILABLE = "/api/v1/catalog/available";
    private static final String INTENTS = "/api/v1/intents";
    private static final String TELEMETRY = "/api/v1/telemetry";

    private final WebClient http;
    private final Duration timeout;

    public ControlPlaneClient(WebClient clientControlPlaneWebClient, ClientConsoleProperties properties) {
        this.http = clientControlPlaneWebClient;
        this.timeout = properties.requestTimeout();
    }

    public Mono<TokenResponse> login(String username, String password) {
        return one(http.post().uri(AUTH + "/login")
                .bodyValue(Map.of("username", username, "password", password)), TokenResponse.class);
    }

    public Flux<AvailableProductView> availableProducts(String token) {
        return many(http.get().uri(AVAILABLE).headers(bearer(token)), AvailableProductView.class);
    }

    public Mono<IntentView> submitIntent(String token, UUID installationId, UUID productId,
                                         String action, UUID targetReleaseId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("installationId", installationId);
        body.put("productId", productId);
        body.put("action", action);
        if (targetReleaseId != null) {
            body.put("targetReleaseId", targetReleaseId);
        }
        return one(http.post().uri(INTENTS).headers(bearer(token)).bodyValue(body), IntentView.class);
    }

    public Flux<IntentView> pendingIntents(String token) {
        return many(http.get().uri(INTENTS).headers(bearer(token)), IntentView.class);
    }

    public Flux<InstallationStatus> installations(String token, int page, int limit) {
        return many(http.get().uri(uri -> paged(uri.path(TELEMETRY + "/installations"), page, limit).build())
                .headers(bearer(token)), InstallationStatus.class);
    }

    public Flux<TelemetryView> events(String token, UUID installationId, int page, int limit) {
        return many(http.get().uri(uri -> paged(uri.path(TELEMETRY + "/events"), page, limit)
                .queryParamIfPresent("installation", Optional.ofNullable(installationId))
                .build()).headers(bearer(token)), TelemetryView.class);
    }

    private static Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }

    private <T> Mono<T> one(WebClient.RequestHeadersSpec<?> request, Class<T> type) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, ControlPlaneClient::asException)
                .bodyToMono(type)
                .timeout(timeout)
                .onErrorMap(ControlPlaneClient::asUnavailable);
    }

    private <T> Flux<T> many(WebClient.RequestHeadersSpec<?> request, Class<T> type) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, ControlPlaneClient::asException)
                .bodyToFlux(type)
                .timeout(timeout)
                .onErrorMap(ControlPlaneClient::asUnavailable);
    }

    private static Throwable asUnavailable(Throwable error) {
        if (error instanceof ControlPlaneException) {
            return error;
        }
        if (error instanceof TimeoutException) {
            return new ControlPlaneException(HttpStatus.GATEWAY_TIMEOUT, "SERVICE_UNAVAILABLE",
                    "Control Plane не ответил вовремя");
        }
        if (error instanceof WebClientRequestException) {
            return new ControlPlaneException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Control Plane недоступен: " + error.getMessage());
        }
        return error;
    }

    private static Mono<Throwable> asException(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(ProblemDetail.class)
                .<Throwable>map(problem -> new ControlPlaneException(status, code(problem), detail(problem, status)))
                .onErrorReturn(new ControlPlaneException(status, null, fallbackDetail(status)))
                .defaultIfEmpty(new ControlPlaneException(status, null, fallbackDetail(status)));
    }

    private static String code(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();
        Object code = properties == null ? null : properties.get("code");
        return code == null ? null : code.toString();
    }

    private static String detail(ProblemDetail problem, HttpStatusCode status) {
        String detail = problem.getDetail();
        return detail == null || detail.isBlank() ? fallbackDetail(status) : detail;
    }

    private static String fallbackDetail(HttpStatusCode status) {
        return "Control Plane вернул " + status;
    }

    private static UriBuilder paged(UriBuilder uri, int page, int limit) {
        return uri.queryParam("page", Math.max(page, 0)).queryParam("limit", limit);
    }
}
