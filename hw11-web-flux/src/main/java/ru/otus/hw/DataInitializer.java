package ru.otus.hw;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AuthorRepository authorRepository;

    private final GenreRepository genreRepository;

    private final BookRepository bookRepository;

    private final CommentRepository commentRepository;

    @Override
    public void run(ApplicationArguments args) {
        authorRepository.count()
                .filter(count -> count == 0)
                .flatMap(count -> initData())
                .block();
    }

    private Mono<Void> initData() {
        var authors = List.of(
                new Author("1", "Author_1"),
                new Author("2", "Author_2"),
                new Author("3", "Author_3"));
        var genres = List.of(
                new Genre("1", "Genre_1"), new Genre("2", "Genre_2"),
                new Genre("3", "Genre_3"), new Genre("4", "Genre_4"),
                new Genre("5", "Genre_5"), new Genre("6", "Genre_6"));
        return authorRepository.saveAll(authors).then()
                .then(genreRepository.saveAll(genres).then())
                .then(insertBooks(authors, genres))
                .then(insertComments());
    }

    private Mono<Void> insertBooks(List<Author> authors, List<Genre> genres) {
        return bookRepository.saveAll(Flux.just(
                new Book("1", "BookTitle_1", authors.get(0), List.of(genres.get(0), genres.get(1))),
                new Book("2", "BookTitle_2", authors.get(1), List.of(genres.get(2), genres.get(3))),
                new Book("3", "BookTitle_3", authors.get(2), List.of(genres.get(4), genres.get(5)))
        )).then();
    }

    private Mono<Void> insertComments() {
        return commentRepository.saveAll(List.of(
                new Comment("1", "Comment_1_Book_1", "1"),
                new Comment("2", "Comment_2_Book_1", "1"),
                new Comment("3", "Comment_1_Book_2", "2"),
                new Comment("4", "Comment_1_Book_3", "3")
        )).then();
    }
}
