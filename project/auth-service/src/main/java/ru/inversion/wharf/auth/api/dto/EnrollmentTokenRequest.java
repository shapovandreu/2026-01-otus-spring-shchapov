package ru.inversion.wharf.auth.api.dto;

import java.time.Duration;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record EnrollmentTokenRequest(@NotNull(message = "обязателен") UUID orgId, Duration ttl) {
}
