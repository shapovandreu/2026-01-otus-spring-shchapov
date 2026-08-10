package ru.inversion.wharf.catalog.domain;

public enum IntentStatus {

    PENDING,

    CONSUMED;

    public String slug() {
        return name().toLowerCase();
    }
}
