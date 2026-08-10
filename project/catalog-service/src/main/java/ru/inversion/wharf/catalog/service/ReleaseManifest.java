package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

public record ReleaseManifest(
        UUID releaseId,
        UUID productId,
        String product,
        String version,
        String channel,
        Instant issuedAt) {
}
