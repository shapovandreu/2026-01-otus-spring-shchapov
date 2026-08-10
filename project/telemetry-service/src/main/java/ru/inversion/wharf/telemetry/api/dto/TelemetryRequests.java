package ru.inversion.wharf.telemetry.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.telemetry.domain.TelemetryEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TelemetryRequests {

    public record TelemetryEvent(
            @NotNull(message = "обязателен") UUID installationId,
            @NotNull(message = "обязателен") UUID productId,
            @NotNull(message = "обязателен") TelemetryEventType type,
            @Size(max = 64, message = "не длиннее 64 символов") String state,
            UUID releaseId,
            @Size(max = 500, message = "не длиннее 500 символов") String message,
            @NotNull(message = "обязателен") Instant occurredAt) {
    }

    private TelemetryRequests() {
    }
}
