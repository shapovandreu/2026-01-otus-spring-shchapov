package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.common.signing.JwsSigner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ManifestService {

    private final CatalogService catalog;
    private final JwsSigner signer;
    private final Cache<UUID, SignedManifest> cache;

    public ManifestService(CatalogService catalog, JwsSigner signer, Cache<UUID, SignedManifest> manifestCache) {
        this.catalog = catalog;
        this.signer = signer;
        this.cache = manifestCache;
    }

    public Mono<SignedManifest> signedManifest(UUID releaseId) {
        SignedManifest cached = cache.getIfPresent(releaseId);
        if (cached != null) {
            return Mono.just(cached);
        }
        return catalog.requirePublished(releaseId)
                .flatMap(this::buildManifest)
                .map(manifest -> new SignedManifest(manifest, signer.sign(manifest)))
                .doOnNext(signed -> cache.put(releaseId, signed));
    }

    public void invalidate(UUID releaseId) {
        cache.invalidate(releaseId);
    }

    private Mono<ReleaseManifest> buildManifest(Release release) {
        return catalog.requireProduct(release.productId())
                .map(product -> new ReleaseManifest(
                        release.id(),
                        product.id(),
                        product.name(),
                        release.version(),
                        release.channel().slug(),
                        Instant.now()));
    }

    public record SignedManifest(ReleaseManifest manifest, String jws) {
    }
}
