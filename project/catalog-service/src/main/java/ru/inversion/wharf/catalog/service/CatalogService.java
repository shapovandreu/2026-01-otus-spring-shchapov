package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.repository.IntentRepository;
import ru.inversion.wharf.catalog.repository.ProductRepository;
import ru.inversion.wharf.catalog.repository.ReleaseRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final ReleaseRepository releases;
    private final IntentRepository intents;

    public CatalogService(ProductRepository products, ReleaseRepository releases, IntentRepository intents) {
        this.products = products;
        this.releases = releases;
        this.intents = intents;
    }

    public Mono<Product> createProduct(String name, String description) {
        return products.save(Product.create(name, description, Instant.now()));
    }

    public Flux<Product> allProducts() {
        return products.findAll();
    }

    public Mono<Product> updateProduct(UUID productId, String name, String description) {
        return requireProduct(productId)
                .flatMap(product -> products.save(product.withDetails(name, description)));
    }

    public Mono<Product> deleteProduct(UUID productId, Mono<Boolean> entitlementsInUse) {
        return requireProduct(productId)
                .flatMap(product -> Mono.zip(
                                releases.countByProductId(productId),
                                intents.countByProductId(productId),
                                entitlementsInUse)
                        .flatMap(refs -> {
                            String blocking = describe(refs.getT1(), refs.getT2(), refs.getT3());
                            return blocking == null
                                    ? products.delete(product).thenReturn(product)
                                    : Mono.error(new CatalogExceptions.ProductInUse(productId, blocking));
                        }));
    }

    private static String describe(long releaseCount, long intentCount, boolean entitlements) {
        StringBuilder blocking = new StringBuilder();
        if (releaseCount > 0) {
            blocking.append("релизы (").append(releaseCount).append(')');
        }
        if (intentCount > 0) {
            append(blocking, "намерения (" + intentCount + ")");
        }
        if (entitlements) {
            append(blocking, "права организаций");
        }
        return blocking.isEmpty() ? null : blocking.toString();
    }

    private static void append(StringBuilder blocking, String what) {
        if (!blocking.isEmpty()) {
            blocking.append(", ");
        }
        blocking.append(what);
    }

    public Mono<Release> createRelease(UUID productId, String version, Channel channel, String changelog) {
        return products.findById(productId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ProductNotFound(productId)))
                .flatMap(product -> releases.save(
                        Release.draft(productId, version, channel, changelog, Instant.now())));
    }

    public Mono<Release> publish(UUID releaseId) {
        return releases.findById(releaseId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ReleaseNotFound(releaseId)))
                .flatMap(release -> release.published()
                        ? Mono.just(release)
                        : releases.save(release.publish()));
    }

    public Mono<Release> changeChannel(UUID releaseId, Channel channel) {
        return releases.findById(releaseId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ReleaseNotFound(releaseId)))
                .flatMap(release -> {
                    if (!release.published()) {
                        return Mono.error(new CatalogExceptions.ReleaseNotPublished(releaseId));
                    }
                    return release.channel() == channel
                            ? Mono.just(release)
                            : releases.save(release.inChannel(channel));
                });
    }

    public Mono<Release> updateDraft(UUID releaseId, String version, Channel channel, String changelog) {
        return requireDraft(releaseId)
                .flatMap(release -> releases.save(release.withDraftDetails(version, channel, changelog)));
    }

    public Mono<Release> deleteDraft(UUID releaseId) {
        return requireDraft(releaseId)
                .flatMap(release -> releases.delete(release).thenReturn(release));
    }

    private Mono<Release> requireDraft(UUID releaseId) {
        return releases.findById(releaseId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ReleaseNotFound(releaseId)))
                .flatMap(release -> release.published()
                        ? Mono.error(new CatalogExceptions.ReleaseAlreadyPublished(releaseId))
                        : Mono.just(release));
    }

    public Flux<Release> releasesForOperator(UUID productId) {
        return releases.findByProductId(productId);
    }

    public Flux<Release> publishedReleases(UUID productId, Channel channel) {
        return channel == null
                ? releases.findByProductIdAndPublishedIsTrue(productId)
                : releases.findByProductIdAndChannelAndPublishedIsTrue(productId, channel);
    }

    public Mono<Release> requirePublished(UUID releaseId) {
        return releases.findById(releaseId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ReleaseNotFound(releaseId)))
                .filter(Release::published)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ReleaseNotPublished(releaseId)));
    }

    public Mono<Product> requireProduct(UUID productId) {
        return products.findById(productId)
                .switchIfEmpty(Mono.error(() -> new CatalogExceptions.ProductNotFound(productId)));
    }
}
