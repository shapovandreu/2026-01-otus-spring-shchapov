package ru.inversion.wharf.auth.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("enrollment_token")
public record EnrollmentToken(
        @Id UUID id,
        UUID orgId,
        String tokenHash,
        Instant expiresAt,
        boolean used,
        boolean revoked,
        Instant createdAt) {

    public static EnrollmentToken issue(UUID orgId, String tokenHash, Instant expiresAt, Instant now) {
        return new EnrollmentToken(null, orgId, tokenHash, expiresAt, false, false, now);
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
