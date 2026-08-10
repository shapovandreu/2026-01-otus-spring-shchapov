package ru.inversion.wharf.agent.fsm;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.agent.verify.ManifestDoc;

public record Installation(
        UUID installationId,
        UUID productId,
        UUID releaseId,
        String version,
        InstallationState state,
        Instant lastSync) {

    public static Installation running(IntentCommand command, ManifestDoc manifest, Instant now) {
        return new Installation(command.installationId(), command.productId(), manifest.releaseId(),
                manifest.version(), InstallationState.RUNNING, now);
    }

    public static Installation rejected(IntentCommand command, Instant now) {
        return new Installation(command.installationId(), command.productId(), command.releaseId(),
                null, InstallationState.REJECTED, now);
    }

    public static Installation removed(Installation current, Instant now) {
        return new Installation(current.installationId(), current.productId(), current.releaseId(),
                current.version(), InstallationState.REMOVED, now);
    }

    public boolean isRunningRelease(UUID candidateReleaseId) {
        return state == InstallationState.RUNNING && candidateReleaseId.equals(releaseId);
    }

    public boolean isRemoved() {
        return state == InstallationState.REMOVED;
    }
}
