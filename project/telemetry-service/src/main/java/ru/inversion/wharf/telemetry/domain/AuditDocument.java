package ru.inversion.wharf.telemetry.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.common.audit.AuditEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_log")
@CompoundIndexes({
        @CompoundIndex(name = "actor_recent", def = "{'actor': 1, 'occurredAt': -1}"),
        @CompoundIndex(name = "action_recent", def = "{'action': 1, 'occurredAt': -1}"),
        @CompoundIndex(name = "org_recent", def = "{'orgId': 1, 'occurredAt': -1}")
})
public record AuditDocument(
        @Id String id,
        String actor,
        List<String> roles,
        String action,
        String targetType,
        String targetId,
        UUID orgId,
        Instant occurredAt) {

    public static AuditDocument from(AuditEvent event) {
        return new AuditDocument(null, event.actor(), event.roles(), event.action(),
                event.targetType(), event.targetId(), event.orgId(), event.occurredAt());
    }
}
