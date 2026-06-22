package ru.otus.hw.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.fault.FaultInjector;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    private final FaultInjector faultInjector;

    @Override
    @Retry(name = "genreService")
    @CircuitBreaker(name = "genreService", fallbackMethod = "findAllFallback")
    public List<Genre> findAll() {
        faultInjector.maybeFail();
        return genreRepository.findAll();
    }

    @SuppressWarnings("unused")
    private List<Genre> findAllFallback(Throwable ex) {
        log.warn("Circuit breaker 'genreService' fallback: возвращаю пустой список жанров. Причина: {}",
                ex.getMessage());
        return List.of();
    }
}
