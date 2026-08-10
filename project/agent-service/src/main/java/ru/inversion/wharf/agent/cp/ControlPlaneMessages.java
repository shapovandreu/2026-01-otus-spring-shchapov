package ru.inversion.wharf.agent.cp;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class ControlPlaneMessages {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentToken(String accessToken, String tokenType, long expiresIn, Instant expiresAt,
                             UUID orgId, String orgName) {
    }

    public record OrgAdminRequest(String username, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrgAdminView(UUID id, String username, UUID orgId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Intent(UUID id, UUID installationId, UUID productId, String action, UUID targetReleaseId,
                         String status, Instant createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SignedDocument(String jws) {
    }

    public record TelemetryEvent(UUID installationId, UUID productId, String type, String state,
                                 UUID releaseId, String message, Instant occurredAt) {
    }

    private ControlPlaneMessages() {
    }
}
