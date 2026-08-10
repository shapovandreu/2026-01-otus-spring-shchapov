package ru.inversion.wharf.console.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ConsoleViews {

    public record OrgView(UUID id, String name, Instant createdAt) {
    }

    public record ProductView(UUID id, String name, String description, Instant createdAt) {
    }

    public record ReleaseView(UUID id, UUID productId, String version, String channel, boolean published,
                              String changelog, Instant createdAt) {
    }

    public record EntitlementView(UUID id, UUID orgId, UUID productId, String channel, Instant validUntil) {
    }

    public record TokenSummaryView(UUID id, UUID orgId, String status, Instant expiresAt, Instant createdAt) {
    }

    public record InstallationStatus(UUID installationId, UUID orgId, UUID productId, String lastType,
                                     String lastState, UUID lastReleaseId, String lastMessage, Instant lastSeen) {
    }

    public record TelemetryView(UUID orgId, UUID agentId, UUID installationId, UUID productId, String type,
                                String state, UUID releaseId, String message, Instant occurredAt,
                                Instant receivedAt) {
    }

    public record AuditView(String id, String actor, List<String> roles, String action, String targetType,
                            String targetId, UUID orgId, Instant occurredAt) {
    }

    public record IssuedTokenView(UUID id, UUID orgId, String token, Instant expiresAt) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt) {
    }

    private ConsoleViews() {
    }
}
