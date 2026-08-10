package ru.inversion.wharf.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("intent")
public record Intent(
        @Id UUID id,
        UUID orgId,
        UUID installationId,
        UUID productId,
        IntentAction action,
        UUID targetReleaseId,
        IntentStatus status,
        Instant createdAt) {

    public static Intent queued(UUID orgId, UUID installationId, UUID productId,
                                IntentAction action, UUID targetReleaseId, Instant now) {
        return new Intent(null, orgId, installationId, productId, action, targetReleaseId,
                IntentStatus.PENDING, now);
    }

    public Intent consumed() {
        return new Intent(id, orgId, installationId, productId, action, targetReleaseId,
                IntentStatus.CONSUMED, createdAt);
    }
}
