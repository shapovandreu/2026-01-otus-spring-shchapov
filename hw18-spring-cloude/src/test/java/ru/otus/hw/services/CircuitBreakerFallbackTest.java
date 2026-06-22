package ru.otus.hw.services;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.otus.hw.fault.FaultInjector;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тест отказоустойчивости: circuit breaker и fallback вокруг вызовов к БД")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CircuitBreakerFallbackTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private FaultInjector faultInjector;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @AfterEach
    void tearDown() {
        faultInjector.disable();
        circuitBreakerRegistry.circuitBreaker("authorService").reset();
    }

    @DisplayName("при недоступности БД должен вернуть fallback (пустой список) и открыть circuit breaker")
    @Test
    void shouldReturnFallbackAndOpenCircuitBreakerWhenDbIsDown() {
        assertThat(authorService.findAll()).isNotEmpty();

        faultInjector.enable();

        for (int i = 0; i < 10; i++) {
            assertThat(authorService.findAll()).isEmpty();
        }

        CircuitBreaker.State state = circuitBreakerRegistry.circuitBreaker("authorService").getState();
        assertThat(state).isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);
    }

    @DisplayName("после восстановления БД circuit breaker возвращается в CLOSED и отдаёт реальные данные")
    @Test
    void shouldRecoverWhenDbIsBackUp() {
        faultInjector.enable();
        for (int i = 0; i < 10; i++) {
            authorService.findAll();
        }
        assertThat(circuitBreakerRegistry.circuitBreaker("authorService").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        faultInjector.disable();
        circuitBreakerRegistry.circuitBreaker("authorService").transitionToClosedState();

        assertThat(authorService.findAll()).isNotEmpty();
    }
}
