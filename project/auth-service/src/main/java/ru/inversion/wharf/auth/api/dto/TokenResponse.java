package ru.inversion.wharf.auth.api.dto;

import java.time.Instant;

import ru.inversion.wharf.auth.service.JwtIssuer;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt) {

    public static TokenResponse of(JwtIssuer.IssuedToken issued) {
        return new TokenResponse(issued.token(), "Bearer", issued.expiresInSeconds(), issued.expiresAt());
    }
}
