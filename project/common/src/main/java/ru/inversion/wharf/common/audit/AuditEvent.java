package ru.inversion.wharf.common.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.common.api.Roles;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuditEvent(
        String actor,
        List<String> roles,
        String action,
        String targetType,
        String targetId,
        UUID orgId,
        Instant occurredAt) {

    public static AuditEvent of(Jwt actor, String action, String targetType, String targetId, UUID orgId) {
        return new AuditEvent(
                actor.getSubject(),
                actor.getClaimAsStringList(Roles.CLAIM_ROLES),
                action,
                targetType,
                targetId,
                orgId,
                Instant.now());
    }
}
