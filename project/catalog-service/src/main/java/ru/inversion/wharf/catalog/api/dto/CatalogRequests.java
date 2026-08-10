package ru.inversion.wharf.catalog.api.dto;

import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.IntentAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CatalogRequests {

    public record CreateProduct(
            @NotBlank(message = "обязательно") String name,
            @Size(max = 500, message = "не длиннее 500 символов") String description) {
    }

    public record CreateRelease(
            @NotBlank(message = "обязательна")
            @Pattern(regexp = "\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?", message = "должна быть semver, например 1.4.2")
            String version,
            Channel channel,
            @Size(max = 1000, message = "не длиннее 1000 символов") String changelog) {

        public Channel channelOrDefault() {
            return channel == null ? Channel.STABLE : channel;
        }
    }

    public record UpdateProduct(
            @NotBlank(message = "обязательно") String name,
            @Size(max = 500, message = "не длиннее 500 символов") String description) {
    }

    public record UpdateRelease(
            @NotBlank(message = "обязательна")
            @Pattern(regexp = "\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?", message = "должна быть semver, например 1.4.2")
            String version,
            Channel channel,
            @Size(max = 1000, message = "не длиннее 1000 символов") String changelog) {

        public Channel channelOrDefault() {
            return channel == null ? Channel.STABLE : channel;
        }
    }

    public record ChangeChannel(@NotNull(message = "обязателен") Channel channel) {
    }

    public record SubmitIntent(
            @NotNull(message = "обязателен") UUID installationId,
            @NotNull(message = "обязателен") UUID productId,
            @NotNull(message = "обязательно") IntentAction action,
            UUID targetReleaseId) {
    }

    private CatalogRequests() {
    }
}
