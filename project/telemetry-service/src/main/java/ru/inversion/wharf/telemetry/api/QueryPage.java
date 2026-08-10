package ru.inversion.wharf.telemetry.api;

import org.springframework.data.domain.PageRequest;

public final class QueryPage {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 500;

    private QueryPage() {
    }

    public static PageRequest of(int page, int limit) {
        return PageRequest.of(Math.max(page, 0), size(limit));
    }

    public static int size(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public static long offset(int page, int limit) {
        return (long) Math.max(page, 0) * size(limit);
    }
}
