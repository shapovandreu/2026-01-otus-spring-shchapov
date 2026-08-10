package ru.inversion.wharf.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("telemetry_events")
@CompoundIndexes({
        @CompoundIndex(name = "org_recent", def = "{'orgId': 1, 'receivedAt': -1}"),
        @CompoundIndex(name = "agent_recent", def = "{'agentId': 1, 'receivedAt': -1}"),
        @CompoundIndex(name = "installation_recent", def = "{'installationId': 1, 'receivedAt': -1}")
})
public record TelemetryDocument(
        @Id String id,
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

    public static TelemetryDocument from(TelemetryRecord record) {
        return new TelemetryDocument(
                null,
                record.orgId(),
                record.agentId(),
                record.installationId(),
                record.productId(),
                record.type(),
                record.state(),
                record.releaseId(),
                record.message(),
                record.occurredAt(),
                record.receivedAt());
    }
}
