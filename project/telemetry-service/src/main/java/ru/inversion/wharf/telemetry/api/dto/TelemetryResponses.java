package ru.inversion.wharf.telemetry.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.telemetry.domain.TelemetryDocument;

public final class TelemetryResponses {

    public record TelemetryView(UUID orgId, UUID agentId, UUID installationId, UUID productId, String type,
                                String state, UUID releaseId, String message, Instant occurredAt,
                                Instant receivedAt) {

        public static TelemetryView of(TelemetryDocument document) {
            return new TelemetryView(document.orgId(), document.agentId(), document.installationId(),
                    document.productId(), document.type(), document.state(), document.releaseId(),
                    document.message(), document.occurredAt(), document.receivedAt());
        }
    }

    private TelemetryResponses() {
    }
}
