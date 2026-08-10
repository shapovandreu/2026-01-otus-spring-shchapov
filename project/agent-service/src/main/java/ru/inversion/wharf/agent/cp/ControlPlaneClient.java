package ru.inversion.wharf.agent.cp;

import java.util.UUID;

import ru.inversion.wharf.agent.cp.ControlPlaneMessages.AgentToken;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.Intent;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.OrgAdminRequest;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.OrgAdminView;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.SignedDocument;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.TelemetryEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ControlPlaneClient {

    private final WebClient web;
    private final AgentIdentity identity;
    private final ControlPlaneResilience resilience;

    public ControlPlaneClient(WebClient controlPlaneWebClient, AgentIdentity identity,
                              ControlPlaneResilience resilience) {
        this.web = controlPlaneWebClient;
        this.identity = identity;
        this.resilience = resilience;
    }

    public Mono<AgentToken> enroll(String enrollmentToken) {
        return resilience.guardWithoutRetry(web.post()
                .uri("/api/v1/auth/enroll")
                .bodyValue(new EnrollBody(enrollmentToken))
                .retrieve()
                .bodyToMono(AgentToken.class));
    }

    public Flux<Intent> intents() {
        return resilience.guard(web.get()
                        .uri("/api/v1/intents")
                        .headers(headers -> headers.setBearerAuth(identity.bearer()))
                        .retrieve()
                        .bodyToFlux(Intent.class)
                        .collectList())
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> consumeIntent(UUID intentId) {
        return resilience.guard(web.post()
                        .uri("/api/v1/intents/{id}/consume", intentId)
                        .headers(headers -> headers.setBearerAuth(identity.bearer()))
                        .retrieve()
                        .toBodilessEntity())
                .then();
    }

    public Mono<OrgAdminView> createOrgAdmin(String username, String password) {
        return resilience.guard(web.post()
                .uri("/api/v1/auth/org-admin")
                .headers(headers -> headers.setBearerAuth(identity.bearer()))
                .bodyValue(new OrgAdminRequest(username, password))
                .retrieve()
                .bodyToMono(OrgAdminView.class));
    }

    public Mono<String> manifestJws(UUID releaseId) {
        return resilience.guard(web.get()
                .uri("/api/v1/releases/{id}/manifest", releaseId)
                .headers(headers -> headers.setBearerAuth(identity.bearer()))
                .retrieve()
                .bodyToMono(SignedDocument.class)
                .map(SignedDocument::jws));
    }

    public Mono<Void> sendTelemetry(TelemetryEvent event) {
        return resilience.guard(web.post()
                        .uri("/api/v1/telemetry")
                        .headers(headers -> headers.setBearerAuth(identity.bearer()))
                        .bodyValue(event)
                        .retrieve()
                        .toBodilessEntity())
                .then();
    }

    public Mono<String> catalogJwks() {
        return web.get().uri("/api/v1/catalog/.well-known/jwks.json").retrieve().bodyToMono(String.class);
    }

    private record EnrollBody(String token) {
    }
}
