package ru.inversion.wharf.license.api.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class LicenseRequests {

    public record GrantEntitlement(
            @NotNull(message = "обязателен") UUID orgId,
            @NotNull(message = "обязателен") UUID productId,
            @Pattern(regexp = "stable|beta", message = "допустимо stable или beta") String channel,
            Instant validUntil) {

        public String channelOrDefault() {
            return channel == null || channel.isBlank() ? "stable" : channel;
        }
    }

    public record UpdateEntitlement(Instant validUntil) {
    }

    private LicenseRequests() {
    }
}
