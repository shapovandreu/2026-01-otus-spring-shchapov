package ru.inversion.wharf.console.client;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import ru.inversion.wharf.console.client.ConsoleViews.AuditView;
import ru.inversion.wharf.console.client.ConsoleViews.EntitlementView;
import ru.inversion.wharf.console.client.ConsoleViews.InstallationStatus;
import ru.inversion.wharf.console.client.ConsoleViews.IssuedTokenView;
import ru.inversion.wharf.console.client.ConsoleViews.OrgView;
import ru.inversion.wharf.console.client.ConsoleViews.ProductView;
import ru.inversion.wharf.console.client.ConsoleViews.ReleaseView;
import ru.inversion.wharf.console.client.ConsoleViews.TelemetryView;
import ru.inversion.wharf.console.client.ConsoleViews.TokenSummaryView;
import ru.inversion.wharf.console.client.ConsoleViews.TokenResponse;
import ru.inversion.wharf.console.config.ConsoleProperties;
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
    private static final String CATALOG = "/api/v1/catalog";
    private static final String ENTITLEMENTS = "/api/v1/entitlements";
    private static final String TELEMETRY = "/api/v1/telemetry";

    private final WebClient http;
    private final Duration timeout;

    public ControlPlaneClient(WebClient controlPlaneWebClient, ConsoleProperties properties) {
        this.http = controlPlaneWebClient;
        this.timeout = properties.requestTimeout();
    }

    public Mono<TokenResponse> login(String username, String password) {
        return one(http.post().uri(AUTH + "/login")
                .bodyValue(Map.of("username", username, "password", password)), TokenResponse.class);
    }

    public Flux<OrgView> organizations(String token) {
        return many(get(token, AUTH + "/orgs"), OrgView.class);
    }

    public Mono<OrgView> createOrganization(String token, String name) {
        return one(post(token, AUTH + "/orgs", Map.of("name", name)), OrgView.class);
    }

    public Mono<OrgView> renameOrganization(String token, UUID orgId, String name) {
        return one(http.put().uri(AUTH + "/orgs/{id}", orgId).headers(bearer(token))
                .bodyValue(Map.of("name", name)), OrgView.class);
    }

    public Mono<Void> deleteOrganization(String token, UUID orgId) {
        return none(http.delete().uri(AUTH + "/orgs/{id}", orgId).headers(bearer(token)));
    }

    public Flux<TokenSummaryView> enrollmentTokens(String token, UUID orgId) {
        return many(http.get().uri(uri -> uri.path(AUTH + "/enrollment-tokens")
                .queryParamIfPresent("orgId", Optional.ofNullable(orgId))
                .build()).headers(bearer(token)), TokenSummaryView.class);
    }

    public Mono<IssuedTokenView> issueEnrollmentToken(String token, UUID orgId, Duration ttl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orgId", orgId);
        if (ttl != null) {
            body.put("ttl", ttl.toString());
        }
        return one(post(token, AUTH + "/enrollment-tokens", body), IssuedTokenView.class);
    }

    public Mono<Void> revokeEnrollmentToken(String token, UUID tokenId) {
        return none(http.delete().uri(AUTH + "/enrollment-tokens/{id}", tokenId).headers(bearer(token)));
    }

    public Flux<ProductView> products(String token) {
        return many(get(token, CATALOG + "/products"), ProductView.class);
    }

    public Mono<ProductView> createProduct(String token, String name, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (description != null) {
            body.put("description", description);
        }
        return one(post(token, CATALOG + "/products", body), ProductView.class);
    }

    public Mono<ProductView> updateProduct(String token, UUID productId, String name, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (description != null) {
            body.put("description", description);
        }
        return one(http.patch().uri(CATALOG + "/products/{id}", productId).headers(bearer(token))
                .bodyValue(body), ProductView.class);
    }

    public Mono<Void> deleteProduct(String token, UUID productId) {
        return none(http.delete().uri(CATALOG + "/products/{id}", productId).headers(bearer(token)));
    }

    public Flux<ReleaseView> releases(String token, UUID productId) {
        return many(http.get().uri(CATALOG + "/products/{id}/releases", productId).headers(bearer(token)),
                ReleaseView.class);
    }

    public Mono<ReleaseView> createRelease(String token, UUID productId, String version, String channel,
                                           String changelog) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", version);
        body.put("channel", channel);
        if (changelog != null) {
            body.put("changelog", changelog);
        }
        return one(http.post().uri(CATALOG + "/products/{id}/releases", productId)
                .headers(bearer(token)).bodyValue(body), ReleaseView.class);
    }

    public Mono<ReleaseView> publishRelease(String token, UUID releaseId) {
        return one(http.post().uri(CATALOG + "/releases/{id}/publish", releaseId).headers(bearer(token)),
                ReleaseView.class);
    }

    public Mono<ReleaseView> changeReleaseChannel(String token, UUID releaseId, String channel) {
        return one(http.patch().uri(CATALOG + "/releases/{id}/channel", releaseId).headers(bearer(token))
                .bodyValue(Map.of("channel", channel)), ReleaseView.class);
    }

    public Mono<ReleaseView> updateRelease(String token, UUID releaseId, String version, String channel,
                                           String changelog) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", version);
        body.put("channel", channel);
        if (changelog != null) {
            body.put("changelog", changelog);
        }
        return one(http.patch().uri(CATALOG + "/releases/{id}", releaseId).headers(bearer(token))
                .bodyValue(body), ReleaseView.class);
    }

    public Mono<Void> deleteRelease(String token, UUID releaseId) {
        return none(http.delete().uri(CATALOG + "/releases/{id}", releaseId).headers(bearer(token)));
    }

    public Flux<EntitlementView> entitlements(String token, UUID orgId) {
        return many(http.get().uri(uri -> uri.path(ENTITLEMENTS)
                .queryParamIfPresent("orgId", Optional.ofNullable(orgId))
                .build()).headers(bearer(token)), EntitlementView.class);
    }

    public Mono<EntitlementView> grantEntitlement(String token, UUID orgId, UUID productId, String channel,
                                                  Instant validUntil) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orgId", orgId);
        body.put("productId", productId);
        body.put("channel", channel);
        if (validUntil != null) {
            body.put("validUntil", validUntil.toString());
        }
        return one(post(token, ENTITLEMENTS, body), EntitlementView.class);
    }

    public Mono<EntitlementView> updateEntitlement(String token, UUID entitlementId, Instant validUntil) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("validUntil", validUntil == null ? null : validUntil.toString());
        return one(http.put().uri(ENTITLEMENTS + "/{id}", entitlementId).headers(bearer(token))
                .bodyValue(body), EntitlementView.class);
    }

    public Mono<Void> revokeEntitlement(String token, UUID entitlementId) {
        return none(http.delete().uri(ENTITLEMENTS + "/{id}", entitlementId).headers(bearer(token)));
    }

    public Flux<InstallationStatus> installations(String token, UUID orgId, int page, int limit) {
        return many(http.get().uri(uri -> paged(uri.path(TELEMETRY + "/installations"), page, limit)
                .queryParamIfPresent("org", Optional.ofNullable(orgId))
                .build()).headers(bearer(token)), InstallationStatus.class);
    }

    public Flux<TelemetryView> events(String token, UUID installationId, UUID orgId, int page, int limit) {
        return many(http.get().uri(uri -> paged(uri.path(TELEMETRY + "/events"), page, limit)
                .queryParamIfPresent("installation", Optional.ofNullable(installationId))
                .queryParamIfPresent("org", Optional.ofNullable(orgId))
                .build()).headers(bearer(token)), TelemetryView.class);
    }

    public Flux<AuditView> audit(String token, String actor, UUID orgId, String action, int page, int limit) {
        return many(http.get().uri(uri -> paged(uri.path(TELEMETRY + "/audit"), page, limit)
                .queryParamIfPresent("actor", blankToEmpty(actor))
                .queryParamIfPresent("org", Optional.ofNullable(orgId))
                .queryParamIfPresent("action", blankToEmpty(action))
                .build()).headers(bearer(token)), AuditView.class);
    }

    private WebClient.RequestHeadersSpec<?> get(String token, String path) {
        return http.get().uri(path).headers(bearer(token));
    }

    private WebClient.RequestHeadersSpec<?> post(String token, String path, Object body) {
        return http.post().uri(path).headers(bearer(token)).bodyValue(body);
    }

    private <T> Mono<T> one(WebClient.RequestHeadersSpec<?> request, Class<T> type) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, ControlPlaneClient::asException)
                .bodyToMono(type)
                .transform(this::guard);
    }

    private <T> Flux<T> many(WebClient.RequestHeadersSpec<?> request, Class<T> type) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, ControlPlaneClient::asException)
                .bodyToFlux(type)
                .timeout(timeout)
                .onErrorMap(ControlPlaneClient::asUnavailable);
    }

    private Mono<Void> none(WebClient.RequestHeadersSpec<?> request) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, ControlPlaneClient::asException)
                .bodyToMono(Void.class)
                .transform(this::guard);
    }

    private <T> Mono<T> guard(Mono<T> call) {
        return call.timeout(timeout).onErrorMap(ControlPlaneClient::asUnavailable);
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

    private static Optional<String> blankToEmpty(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }
}
