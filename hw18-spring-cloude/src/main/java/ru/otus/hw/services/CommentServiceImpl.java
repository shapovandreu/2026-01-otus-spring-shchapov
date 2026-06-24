package ru.otus.hw.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.fault.FaultInjector;
import ru.otus.hw.models.Comment;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    private final BookRepository bookRepository;

    private final FaultInjector faultInjector;

    @Override
    @Transactional(readOnly = true)
    @Retry(name = "commentService")
    @CircuitBreaker(name = "commentService", fallbackMethod = "findByIdFallback")
    public Optional<Comment> findById(long id) {
        faultInjector.maybeFail();
        return commentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Retry(name = "commentService")
    @CircuitBreaker(name = "commentService", fallbackMethod = "findByBookIdFallback")
    public List<Comment> findByBookId(long bookId) {
        faultInjector.maybeFail();
        bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book with id %d not found".formatted(bookId)));
        return commentRepository.findByBookId(bookId);
    }

    @SuppressWarnings("unused")
    private Optional<Comment> findByIdFallback(long id, Throwable ex) {
        log.warn("Circuit breaker 'commentService' fallback: комментарий id={} недоступен. Причина: {}",
                id, ex.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private List<Comment> findByBookIdFallback(long bookId, Throwable ex) {
        log.warn("Circuit breaker 'commentService' fallback: комментарии книги id={} недоступны. Причина: {}",
                bookId, ex.getMessage());
        return List.of();
    }

    @Override
    @Transactional
    public Comment create(long bookId, String text) {
        return save(0L, text, bookId);
    }

    @Override
    @Transactional
    public Comment update(long id, String text, long bookId) {
        return save(id, text, bookId);
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        commentRepository.deleteById(id);
    }

    private Comment save(long id, String text, long bookId) {
        var book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book with id %d not found".formatted(bookId)));
        var comment = new Comment(id, text, book);
        return commentRepository.save(comment);
    }

}