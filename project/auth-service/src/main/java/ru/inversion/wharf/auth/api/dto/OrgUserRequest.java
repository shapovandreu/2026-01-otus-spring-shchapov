package ru.inversion.wharf.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrgUserRequest(
        @NotBlank(message = "обязателен")
        @Size(max = 100, message = "не длиннее 100 символов")
        String username,

        @NotBlank(message = "обязателен")
        @Size(min = 8, max = 200, message = "от 8 до 200 символов")
        String password) {
}
