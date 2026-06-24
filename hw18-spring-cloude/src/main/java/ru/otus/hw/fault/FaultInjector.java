package ru.otus.hw.fault;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Эмулятор недоступности БД. Когда "сбой" включён, вызов {@link #maybeFail()}
 * бросает исключение уровня доступа к данным — это позволяет продемонстрировать
 * срабатывание circuit breaker'а на внешних вызовах (обращениях к БД).
 */
@Component
public class FaultInjector {

    private final AtomicBoolean failing = new AtomicBoolean(false);

    public void enable() {
        failing.set(true);
    }

    public void disable() {
        failing.set(false);
    }

    public boolean isFailing() {
        return failing.get();
    }

    public void maybeFail() {
        if (failing.get()) {
            throw new DataAccessResourceFailureException("Эмуляция недоступности БД (fault injection включён)");
        }
    }
}
