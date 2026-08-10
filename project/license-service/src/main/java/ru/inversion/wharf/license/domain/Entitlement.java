package ru.inversion.wharf.license.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("entitlement")
public record Entitlement(
        @Id UUID id,
        UUID orgId,
        UUID productId,
        String channel,
        Instant validUntil,
        Instant createdAt) {

    public static Entitlement grant(UUID orgId, UUID productId, String channel, Instant validUntil, Instant now) {
        return new Entitlement(null, orgId, productId, channel, validUntil, now);
    }

    public boolean isValid(Instant now) {
        return validUntil == null || now.isBefore(validUntil);
    }

    public Entitlement validUntil(Instant newValidUntil) {
        return new Entitlement(id, orgId, productId, channel, newValidUntil, createdAt);
    }
}
