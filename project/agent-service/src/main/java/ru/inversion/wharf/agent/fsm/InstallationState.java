package ru.inversion.wharf.agent.fsm;

public enum InstallationState {

    INTENDED,
    VERIFYING,
    PULLING,
    DEPLOYING,
    RUNNING,
    ROLLING_BACK,
    REJECTED,
    FAILED,

    REMOVED
}
