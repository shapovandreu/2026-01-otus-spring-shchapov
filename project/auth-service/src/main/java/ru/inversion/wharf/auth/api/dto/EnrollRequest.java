package ru.inversion.wharf.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollRequest(@NotBlank(message = "обязателен") String token) {
}
