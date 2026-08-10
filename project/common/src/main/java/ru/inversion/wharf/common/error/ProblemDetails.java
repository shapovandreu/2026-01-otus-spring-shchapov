package ru.inversion.wharf.common.error;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create(code.type()));
        problem.setTitle(code.slug());
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    public static ProblemDetail of(DomainException exception) {
        return of(exception.code(), exception.getMessage());
    }

    private ProblemDetails() {
    }
}
