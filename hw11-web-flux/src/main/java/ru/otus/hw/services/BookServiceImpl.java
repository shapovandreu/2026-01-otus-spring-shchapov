package ru.otus.hw.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Book;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookService {

    private final AuthorRepository authorRepository;

    private final GenreRepository genreRepository;

    private final BookRepository bookRepository;

    private final CommentRepository commentRepository;

    @Override
    public Mono<Book> findById(String id) {
        return bookRepository.findById(id);
    }

    @Override
    public Flux<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Mono<Book> insert(String title, String authorId, Set<String> genreIds) {
        return save(null, title, authorId, genreIds);
    }

    @Override
    public Mono<Book> update(String id, String title, String authorId, Set<String> genreIds) {
        return save(id, title, authorId, genreIds);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return commentRepository.deleteByBookId(id)
                .then(bookRepository.deleteById(id));
    }

    private Mono<Book> save(String id, String title, String authorId, Set<String> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Genres ids must not be null"));
        }
        return Mono.zip(fetchAuthor(authorId), fetchGenres(genreIds))
                .flatMap(tuple -> {
                    var book = new Book(id, title, tuple.getT1(), tuple.getT2());
                    return bookRepository.save(book);
                });
    }

    private Mono<ru.otus.hw.models.Author> fetchAuthor(String authorId) {
        return authorRepository.findById(authorId)
                .switchIfEmpty(Mono.error(
                        new EntityNotFoundException("Author with id %s not found".formatted(authorId))));
    }

    private Mono<java.util.List<ru.otus.hw.models.Genre>> fetchGenres(Set<String> genreIds) {
        return genreRepository.findAllByIdIn(genreIds)
                .collectList()
                .flatMap(genres -> {
                    if (genres.size() != genreIds.size()) {
                        return Mono.error(new EntityNotFoundException(
                                "One or all genres with ids %s not found".formatted(genreIds)));
                    }
                    return Mono.just(genres);
                });
    }
}
