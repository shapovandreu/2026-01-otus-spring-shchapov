package ru.inversion.wharf.auth.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("operator_user")
public record OperatorUser(
        @Id UUID id,
        String username,
        String passwordHash,
        String roles,
        UUID orgId,
        Instant createdAt) {

    public List<String> roleList() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }

    public boolean belongsToOrganization() {
        return orgId != null;
    }

    public static OperatorUser create(String username, String passwordHash, String roles, Instant now) {
        return new OperatorUser(null, username, passwordHash, roles, null, now);
    }

    public static OperatorUser forOrganization(String username, String passwordHash, String roles,
                                               UUID orgId, Instant now) {
        return new OperatorUser(null, username, passwordHash, roles, orgId, now);
    }

    public OperatorUser withPasswordHash(String newPasswordHash) {
        return new OperatorUser(id, username, newPasswordHash, roles, orgId, createdAt);
    }
}
