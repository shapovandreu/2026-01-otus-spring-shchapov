package ru.inversion.wharf.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED("validation-failed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND),
    FORBIDDEN("forbidden", HttpStatus.FORBIDDEN),
    ALREADY_EXISTS("already-exists", HttpStatus.CONFLICT),

    INVALID_CREDENTIALS("invalid-credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("token-invalid", HttpStatus.UNAUTHORIZED),

    TOKEN_ALREADY_USED("token-already-used", HttpStatus.CONFLICT),
    TOKEN_EXPIRED("token-expired", HttpStatus.GONE),
    ORGANIZATION_IN_USE("organization-in-use", HttpStatus.CONFLICT),
    PRODUCT_IN_USE("product-in-use", HttpStatus.CONFLICT),
    RELEASE_PUBLISHED("release-published", HttpStatus.CONFLICT),
    SIGNATURE_INVALID("signature-invalid", HttpStatus.UNPROCESSABLE_ENTITY),

    SERVICE_UNAVAILABLE("service-unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private static final String TYPE_BASE = "https://inversionwharf.com/errors/";

    private final String slug;
    private final HttpStatus status;

    ErrorCode(String slug, HttpStatus status) {
        this.slug = slug;
        this.status = status;
    }

    public String slug() {
        return slug;
    }

    public HttpStatus status() {
        return status;
    }

    public String type() {
        return TYPE_BASE + slug;
    }
}
