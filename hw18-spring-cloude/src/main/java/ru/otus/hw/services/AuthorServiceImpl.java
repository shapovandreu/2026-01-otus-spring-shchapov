package ru.otus.hw.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.fault.FaultInjector;
import ru.otus.hw.models.Author;
import ru.otus.hw.repositories.AuthorRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    private final FaultInjector faultInjector;

    @Override
    @Retry(name = "authorService")
    @CircuitBreaker(name = "authorService", fallbackMethod = "findAllFallback")
    public List<Author> findAll() {
        faultInjector.maybeFail();
        return authorRepository.findAll();
    }

    @SuppressWarnings("unused")
    private List<Author> findAllFallback(Throwable ex) {
        log.warn("Circuit breaker 'authorService' fallback: возвращаю пустой список авторов. Причина: {}",
                ex.getMessage());
        return List.of();
    }
}
