package ru.inversion.wharf.console.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iw.console")
public record ConsoleProperties(
        @NotBlank(message = "обязателен: без адреса шлюза консоль неработоспособна")
        String gatewayUrl,

        @NotNull(message = "обязателен")
        Duration requestTimeout,

        @NotNull(message = "обязательна")
        @Valid
        Session session) {

    public record Session(
            @NotBlank(message = "обязательно")
            String cookieName,

            boolean secure) {
    }
}
