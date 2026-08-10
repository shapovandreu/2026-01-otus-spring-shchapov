package ru.inversion.wharf.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.telemetry.api.dto.TelemetryRequests.TelemetryEvent;

public record TelemetryRecord(
        UUID orgId,
        UUID agentId,
        UUID installationId,
        UUID productId,
        String type,
        String state,
        UUID releaseId,
        String message,
        Instant occurredAt,
        Instant receivedAt) {

    public static TelemetryRecord from(UUID orgId, UUID agentId, TelemetryEvent event, Instant receivedAt) {
        return new TelemetryRecord(
                orgId,
                agentId,
                event.installationId(),
                event.productId(),
                event.type().slug(),
                event.state(),
                event.releaseId(),
                event.message(),
                event.occurredAt(),
                receivedAt);
    }
}
