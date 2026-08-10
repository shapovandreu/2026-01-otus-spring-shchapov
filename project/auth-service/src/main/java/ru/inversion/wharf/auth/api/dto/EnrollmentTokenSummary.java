package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.EnrollmentToken;

public record EnrollmentTokenSummary(UUID id, UUID orgId, Status status, Instant expiresAt, Instant createdAt) {

    public enum Status {
        ACTIVE,
        USED,
        REVOKED,
        EXPIRED
    }

    public static EnrollmentTokenSummary of(EnrollmentToken token, Instant now) {
        return new EnrollmentTokenSummary(token.id(), token.orgId(), statusOf(token, now),
                token.expiresAt(), token.createdAt());
    }

    private static Status statusOf(EnrollmentToken token, Instant now) {
        if (token.revoked()) {
            return Status.REVOKED;
        }
        if (token.used()) {
            return Status.USED;
        }
        return token.isExpired(now) ? Status.EXPIRED : Status.ACTIVE;
    }
}
