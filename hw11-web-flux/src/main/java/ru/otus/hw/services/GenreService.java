package ru.otus.hw.services;

import reactor.core.publisher.Flux;
import ru.otus.hw.models.Genre;

public interface GenreService {
    Flux<Genre> findAll();
}
