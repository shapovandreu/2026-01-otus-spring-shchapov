package ru.inversion.wharf.auth.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("agent")
public record Agent(
        @Id UUID id,
        UUID orgId,
        String status,
        Instant enrolledAt,
        Instant lastSeen) {

    public static final String STATUS_ACTIVE = "active";

    public static Agent enrolled(UUID orgId, Instant now) {
        return new Agent(null, orgId, STATUS_ACTIVE, now, null);
    }
}
