package ru.inversion.wharf.common.api;

public final class ApiVersion {

    public static final String V1 = "v1";

    public static final String V1_PREFIX = "/api/" + V1;

    public static final String HEADER = "X-IW-Api-Version";

    private ApiVersion() {
    }
}
