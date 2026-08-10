package ru.inversion.wharf.gateway.fallback;

import ru.inversion.wharf.common.error.ErrorCode;
import ru.inversion.wharf.common.error.ProblemDetails;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    private static final String RETRY_AFTER_SECONDS = "10";

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<ProblemDetail>> fallback(@PathVariable String service) {
        ProblemDetail problem = ProblemDetails.of(
                ErrorCode.SERVICE_UNAVAILABLE,
                "Сервис '%s' временно недоступен, повторите позже".formatted(service));

        return Mono.just(ResponseEntity
                .status(ErrorCode.SERVICE_UNAVAILABLE.status())
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }
}
