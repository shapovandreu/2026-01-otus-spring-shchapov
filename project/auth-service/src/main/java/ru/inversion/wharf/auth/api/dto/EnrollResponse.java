package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.service.EnrollmentService;

public record EnrollResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt,
                             UUID orgId, String orgName) {

    public static EnrollResponse of(EnrollmentService.EnrolledAgent enrolled) {
        return new EnrollResponse(enrolled.token().token(), "Bearer", enrolled.token().expiresInSeconds(),
                enrolled.token().expiresAt(), enrolled.orgId(), enrolled.orgName());
    }
}
