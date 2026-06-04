package ru.otus.hw.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.models.Comment;

public interface CommentService {

    Mono<Comment> findById(String id);

    Flux<Comment> findByBookId(String bookId);

    Mono<Comment> create(String bookId, String text);

    Mono<Comment> update(String id, String text, String bookId);

    Mono<Void> deleteById(String id);
}
