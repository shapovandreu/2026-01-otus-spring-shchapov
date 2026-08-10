package ru.inversion.wharf.catalog.domain;

public enum Channel {

    STABLE,
    BETA;

    public String slug() {
        return name().toLowerCase();
    }

    public static Channel fromSlug(String slug) {
        return valueOf(slug.toUpperCase());
    }
}
