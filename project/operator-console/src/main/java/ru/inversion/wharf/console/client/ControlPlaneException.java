package ru.inversion.wharf.console.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class ControlPlaneException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;

    public ControlPlaneException(HttpStatusCode status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatusCode status() {
        return status;
    }

    public String code() {
        return code;
    }

    public boolean isUnauthorized() {
        return status.isSameCodeAs(HttpStatus.UNAUTHORIZED);
    }
}
