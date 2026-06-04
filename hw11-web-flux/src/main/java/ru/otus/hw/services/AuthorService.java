package ru.otus.hw.services;

import reactor.core.publisher.Flux;
import ru.otus.hw.models.Author;

public interface AuthorService {
    Flux<Author> findAll();
}
