package ru.otus.hw.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Comment;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    private final BookRepository bookRepository;

    @Override
    public Mono<Comment> findById(String id) {
        return commentRepository.findById(id);
    }

    @Override
    public Flux<Comment> findByBookId(String bookId) {
        return bookRepository.existsById(bookId)
                .flatMapMany(exists -> {
                    if (!exists) {
                        return Flux.error(new EntityNotFoundException(
                                "Book with id %s not found".formatted(bookId)));
                    }
                    return commentRepository.findByBookId(bookId);
                });
    }

    @Override
    public Mono<Comment> create(String bookId, String text) {
        return save(null, text, bookId);
    }

    @Override
    public Mono<Comment> update(String id, String text, String bookId) {
        return save(id, text, bookId);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return commentRepository.deleteById(id);
    }

    private Mono<Comment> save(String id, String text, String bookId) {
        return bookRepository.findById(bookId)
                .switchIfEmpty(Mono.error(
                        new EntityNotFoundException("Book with id %s not found".formatted(bookId))))
                .flatMap(book -> commentRepository.save(new Comment(id, text, bookId)));
    }
}
