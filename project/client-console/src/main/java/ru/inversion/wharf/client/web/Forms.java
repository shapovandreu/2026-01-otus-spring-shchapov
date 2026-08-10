package ru.inversion.wharf.client.web;

import java.util.Set;
import java.util.UUID;

import org.springframework.util.MultiValueMap;

public final class Forms {

    private final MultiValueMap<String, String> data;

    private Forms(MultiValueMap<String, String> data) {
        this.data = data;
    }

    public static Forms of(MultiValueMap<String, String> data) {
        return new Forms(data);
    }

    public String text(String field) {
        String value = optionalText(field);
        if (value == null) {
            throw new FormException("Поле «" + field + "» обязательно");
        }
        return value;
    }

    public String optionalText(String field) {
        String value = data.getFirst(field);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID uuid(String field) {
        UUID value = optionalUuid(field);
        if (value == null) {
            throw new FormException("Поле «" + field + "» обязательно");
        }
        return value;
    }

    public UUID optionalUuid(String field) {
        String value = optionalText(field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new FormException("Поле «" + field + "» должно быть UUID, а не «" + value + "»");
        }
    }

    public String choice(String field, Set<String> allowed, String fallback) {
        String value = optionalText(field);
        if (value == null) {
            return fallback;
        }
        if (!allowed.contains(value)) {
            throw new FormException("Недопустимое значение поля «" + field + "»: " + value);
        }
        return value;
    }

    public static class FormException extends RuntimeException {
        public FormException(String message) {
            super(message);
        }
    }
}
