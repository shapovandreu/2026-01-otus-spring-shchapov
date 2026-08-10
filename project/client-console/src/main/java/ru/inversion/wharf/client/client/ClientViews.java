package ru.inversion.wharf.client.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class ClientViews {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(String accessToken, Instant expiresAt, long expiresInSeconds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseView(UUID id, UUID productId, String version, String channel, boolean published,
                              String changelog, Instant createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AvailableProductView(UUID productId, String name, String description, String channel,
                                       Instant entitledUntil, List<ReleaseView> releases,
                                       UUID latestReleaseId) {

        public boolean hasReleases() {
            return releases != null && !releases.isEmpty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntentView(UUID id, UUID installationId, UUID productId, String action,
                             UUID targetReleaseId, String status, Instant createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstallationStatus(UUID installationId, UUID orgId, UUID productId, String lastType,
                                     String lastState, UUID lastReleaseId, String lastMessage,
                                     Instant lastSeen) {

        public boolean isRunning() {
            return "RUNNING".equalsIgnoreCase(lastState);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelemetryView(UUID orgId, UUID agentId, UUID installationId, UUID productId, String type,
                                String state, UUID releaseId, String message,
                                Instant occurredAt, Instant receivedAt) {
    }

    private ClientViews() {
    }
}
