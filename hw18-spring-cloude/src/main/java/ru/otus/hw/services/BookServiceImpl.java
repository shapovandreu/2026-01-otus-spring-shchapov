package ru.otus.hw.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.fault.FaultInjector;
import ru.otus.hw.models.Book;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookService {

    private final AuthorRepository authorRepository;

    private final GenreRepository genreRepository;

    private final BookRepository bookRepository;

    private final FaultInjector faultInjector;

    @Override
    @Retry(name = "bookService")
    @CircuitBreaker(name = "bookService", fallbackMethod = "findByIdFallback")
    public Optional<Book> findById(long id) {
        faultInjector.maybeFail();
        return bookRepository.findById(id);
    }

    @Override
    @Retry(name = "bookService")
    @CircuitBreaker(name = "bookService", fallbackMethod = "findAllFallback")
    public List<Book> findAll() {
        faultInjector.maybeFail();
        return bookRepository.findAll();
    }

    @SuppressWarnings("unused")
    private Optional<Book> findByIdFallback(long id, Throwable ex) {
        log.warn("Circuit breaker 'bookService' fallback: книга id={} недоступна. Причина: {}", id, ex.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private List<Book> findAllFallback(Throwable ex) {
        log.warn("Circuit breaker 'bookService' fallback: возвращаю пустой список книг. Причина: {}", ex.getMessage());
        return List.of();
    }

    @Override
    @Transactional
    public Book insert(String title, long authorId, Set<Long> genresIds) {
        return save(0, title, authorId, genresIds);
    }

    @Override
    @Transactional
    public Book update(long id, String title, long authorId, Set<Long> genresIds) {
        return save(id, title, authorId, genresIds);
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        bookRepository.deleteById(id);
    }

    private Book save(long id, String title, long authorId, Set<Long> genresIds) {
        if (isEmpty(genresIds)) {
            throw new IllegalArgumentException("Genres ids must not be null");
        }

        var author = authorRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author with id %d not found".formatted(authorId)));
        var genres = genreRepository.findAllByIds(genresIds);
        if (isEmpty(genres) || genresIds.size() != genres.size()) {
            throw new EntityNotFoundException("One or all genres with ids %s not found".formatted(genresIds));
        }

        var book = new Book(id, title, author, genres);
        return bookRepository.save(book);
    }

}