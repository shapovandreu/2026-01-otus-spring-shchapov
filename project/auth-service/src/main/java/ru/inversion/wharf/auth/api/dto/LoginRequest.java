package ru.inversion.wharf.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "обязателен") String username,
        @NotBlank(message = "обязателен") String password) {
}
