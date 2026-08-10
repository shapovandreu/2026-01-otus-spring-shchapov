package ru.inversion.wharf.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank(message = "обязательно")
        @Size(max = 200, message = "не длиннее 200 символов")
        String name) {
}
