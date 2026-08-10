package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.Organization;

public record OrganizationResponse(UUID id, String name, Instant createdAt) {

    public static OrganizationResponse of(Organization organization) {
        return new OrganizationResponse(organization.id(), organization.name(), organization.createdAt());
    }
}
