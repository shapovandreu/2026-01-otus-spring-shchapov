package ru.inversion.wharf.agent.cp;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ControlPlaneResilience {

    private static final String INSTANCE = "controlPlane";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public ControlPlaneResilience(CircuitBreakerRegistry circuitBreakers,
                                  RetryRegistry retries,
                                  TimeLimiterRegistry timeLimiters) {
        this.circuitBreaker = circuitBreakers.circuitBreaker(INSTANCE);
        this.retry = retries.retry(INSTANCE);
        this.timeLimiter = timeLimiters.timeLimiter(INSTANCE);
    }

    public <T> Mono<T> guard(Mono<T> call) {
        return call
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }

    public <T> Mono<T> guardWithoutRetry(Mono<T> call) {
        return call
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    public CircuitBreaker circuitBreaker() {
        return circuitBreaker;
    }
}
