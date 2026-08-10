package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.catalog.error.CatalogExceptions;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EntitlementGate {

    private final WebClient licenseClient;

    public EntitlementGate(WebClient licenseClient) {
        this.licenseClient = licenseClient;
    }

    public Mono<Boolean> isAllowed(UUID orgId, UUID productId, String channel, String bearerToken) {
        return licenseClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/entitlements/check")
                        .queryParam("orgId", orgId)
                        .queryParam("productId", productId)
                        .queryParam("channel", channel)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(EntitlementCheck.class)
                .map(EntitlementCheck::allowed);
    }

    public Mono<Boolean> isProductInUse(UUID productId, String bearerToken) {
        return licenseClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/entitlements/in-use")
                        .queryParam("productId", productId)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(ProductInUse.class)
                .map(ProductInUse::inUse);
    }

    public Mono<Void> require(UUID orgId, UUID productId, String channel, String bearerToken) {
        return isAllowed(orgId, productId, channel, bearerToken)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.EntitlementMissing(productId, channel)))
                .then();
    }

    public Flux<EntitlementView> mine(String bearerToken) {
        return licenseClient.get()
                .uri("/api/v1/entitlements/mine")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToFlux(EntitlementView.class);
    }

    private record EntitlementCheck(boolean allowed) {
    }

    private record ProductInUse(boolean inUse) {
    }

    public record EntitlementView(UUID productId, String channel, Instant validUntil) {
    }
}
