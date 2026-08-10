package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Intent;
import ru.inversion.wharf.catalog.domain.IntentAction;
import ru.inversion.wharf.catalog.domain.IntentStatus;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.repository.IntentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class IntentService {

    private final IntentRepository intents;
    private final CatalogService catalog;

    public IntentService(IntentRepository intents, CatalogService catalog) {
        this.intents = intents;
        this.catalog = catalog;
    }

    public Mono<Intent> submit(UUID orgId, UUID installationId, UUID productId,
                               IntentAction action, UUID targetReleaseId) {
        if (!action.needsTargetRelease()) {
            return catalog.requireProduct(productId)
                    .then(intents.save(Intent.queued(orgId, installationId, productId, action,
                            null, Instant.now())));
        }
        if (targetReleaseId == null) {
            return Mono.error(new CatalogExceptions.TargetReleaseRequired(action.slug()));
        }
        return catalog.requireProduct(productId)
                .then(catalog.requirePublished(targetReleaseId))
                .flatMap(release -> release.productId().equals(productId)
                        ? intents.save(Intent.queued(orgId, installationId, productId, action,
                                targetReleaseId, Instant.now()))
                        : Mono.error(new CatalogExceptions.ReleaseProductMismatch(targetReleaseId, productId)));
    }

    public Flux<Intent> pending(UUID orgId) {
        return intents.findByOrgIdAndStatusOrderByCreatedAt(orgId, IntentStatus.PENDING);
    }

    public Mono<Intent> markConsumed(UUID intentId, UUID orgId) {
        return intents.findById(intentId)
                .filter(intent -> intent.orgId().equals(orgId))
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.IntentNotFound(intentId)))
                .flatMap(intent -> intents.save(intent.consumed()));
    }
}
