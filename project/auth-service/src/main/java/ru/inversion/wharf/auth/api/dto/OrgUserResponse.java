package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;

public record OrgUserResponse(UUID id, String username, List<String> roles, UUID orgId, Instant createdAt) {

    public static OrgUserResponse of(OperatorUser user) {
        return new OrgUserResponse(user.id(), user.username(), user.roleList(), user.orgId(), user.createdAt());
    }
}
