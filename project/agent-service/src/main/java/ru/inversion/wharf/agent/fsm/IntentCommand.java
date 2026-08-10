package ru.inversion.wharf.agent.fsm;

import java.util.UUID;

import ru.inversion.wharf.agent.cp.ControlPlaneMessages.Intent;

public record IntentCommand(UUID id, Action action, UUID installationId, UUID productId, UUID releaseId) {

    public enum Action {
        INSTALL,
        UPDATE,
        ROLLBACK,
        UNINSTALL
    }

    public static IntentCommand from(Intent intent) {
        return new IntentCommand(
                intent.id(),
                Action.valueOf(intent.action().toUpperCase()),
                intent.installationId(),
                intent.productId(),
                intent.targetReleaseId());
    }

    public boolean isRollback() {
        return action == Action.ROLLBACK;
    }

    public boolean isUninstall() {
        return action == Action.UNINSTALL;
    }
}
