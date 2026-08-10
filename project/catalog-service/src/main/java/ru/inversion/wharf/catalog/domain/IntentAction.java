package ru.inversion.wharf.catalog.domain;

public enum IntentAction {

    INSTALL,

    UPDATE,

    ROLLBACK,

    UNINSTALL;

    public boolean needsTargetRelease() {
        return this != UNINSTALL;
    }

    public String slug() {
        return name().toLowerCase();
    }

    public static IntentAction fromSlug(String slug) {
        return valueOf(slug.toUpperCase());
    }
}
