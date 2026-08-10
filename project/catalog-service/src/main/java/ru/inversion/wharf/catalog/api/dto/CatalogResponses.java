package ru.inversion.wharf.catalog.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Intent;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.catalog.service.ManifestService;
import ru.inversion.wharf.catalog.service.ReleaseManifest;

public final class CatalogResponses {

    public record ProductView(UUID id, String name, String description, Instant createdAt) {

        public static ProductView of(Product product) {
            return new ProductView(product.id(), product.name(), product.description(), product.createdAt());
        }
    }

    public record ReleaseView(UUID id, UUID productId, String version, String channel, boolean published,
                              String changelog, Instant createdAt) {

        public static ReleaseView of(Release release) {
            return new ReleaseView(release.id(), release.productId(), release.version(),
                    release.channel().slug(), release.published(), release.changelog(), release.createdAt());
        }
    }

    public record SignedManifestView(ReleaseManifest manifest, String jws) {

        public static SignedManifestView of(ManifestService.SignedManifest signed) {
            return new SignedManifestView(signed.manifest(), signed.jws());
        }
    }

    public record AvailableProductView(UUID productId, String name, String description, String channel,
                                       Instant entitledUntil, List<ReleaseView> releases,
                                       UUID latestReleaseId) {
    }

    public record IntentView(UUID id, UUID installationId, UUID productId, String action,
                             UUID targetReleaseId, String status, Instant createdAt) {

        public static IntentView of(Intent intent) {
            return new IntentView(intent.id(), intent.installationId(), intent.productId(),
                    intent.action().slug(), intent.targetReleaseId(), intent.status().slug(),
                    intent.createdAt());
        }
    }

    private CatalogResponses() {
    }
}
