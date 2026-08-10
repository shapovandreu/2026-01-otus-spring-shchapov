package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.service.EnrollmentService;

public record EnrollmentTokenResponse(UUID id, UUID orgId, String token, Instant expiresAt) {

    public static EnrollmentTokenResponse of(EnrollmentService.IssuedEnrollmentToken issued) {
        return new EnrollmentTokenResponse(issued.id(), issued.orgId(), issued.secret(), issued.expiresAt());
    }
}
