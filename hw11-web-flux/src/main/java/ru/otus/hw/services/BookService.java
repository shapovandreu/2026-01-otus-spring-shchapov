package ru.otus.hw.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.models.Book;

import java.util.Set;

public interface BookService {

    Mono<Book> findById(String id);

    Flux<Book> findAll();

    Mono<Book> insert(String title, String authorId, Set<String> genreIds);

    Mono<Book> update(String id, String title, String authorId, Set<String> genreIds);

    Mono<Void> deleteById(String id);
}
