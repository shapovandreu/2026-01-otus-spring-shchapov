package ru.inversion.wharf.telemetry.domain;

public enum TelemetryEventType {

    STATE_CHANGED,

    VERIFY_FAILED,

    INTENT_EXECUTED;

    public String slug() {
        return name().toLowerCase();
    }
}
