package ru.inversion.wharf.telemetry.query;

import java.time.Instant;
import java.util.UUID;

public record InstallationStatus(
        UUID installationId,
        UUID orgId,
        UUID productId,
        String lastType,
        String lastState,
        UUID lastReleaseId,
        String lastMessage,
        Instant lastSeen) {
}
