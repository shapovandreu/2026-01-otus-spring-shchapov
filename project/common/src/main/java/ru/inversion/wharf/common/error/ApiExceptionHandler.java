package ru.inversion.wharf.common.error;

import java.util.stream.Collectors;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> onDomain(DomainException exception) {
        return respond(ProblemDetails.of(exception));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> onValidation(WebExchangeBindException exception) {
        String detail = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(ProblemDetails.of(ErrorCode.VALIDATION_FAILED, detail));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> onAccessDenied(AccessDeniedException exception) {
        return respond(ProblemDetails.of(ErrorCode.FORBIDDEN, "Недостаточно прав для операции"));
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
