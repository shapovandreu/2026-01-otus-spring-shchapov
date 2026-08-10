package ru.inversion.wharf.agent.verify;

import java.time.Instant;
import java.util.UUID;

public record ManifestDoc(
        UUID releaseId,
        UUID productId,
        String product,
        String version,
        String channel,
        Instant issuedAt) {
}
