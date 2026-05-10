package ru.otus.hw;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
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
public class DataInitializer implements CommandLineRunner {

    private final AuthorRepository authorRepository;

    private final GenreRepository genreRepository;

    private final BookRepository bookRepository;

    private final CommentRepository commentRepository;

    @Override
    public void run(String... args) {
        if (authorRepository.count() > 0) {
            return;
        }

        var author1 = authorRepository.save(new Author(null, "Author_1"));
        var author2 = authorRepository.save(new Author(null, "Author_2"));
        var author3 = authorRepository.save(new Author(null, "Author_3"));

        var genre1 = genreRepository.save(new Genre(null, "Genre_1"));
        var genre2 = genreRepository.save(new Genre(null, "Genre_2"));
        var genre3 = genreRepository.save(new Genre(null, "Genre_3"));
        var genre4 = genreRepository.save(new Genre(null, "Genre_4"));
        var genre5 = genreRepository.save(new Genre(null, "Genre_5"));
        var genre6 = genreRepository.save(new Genre(null, "Genre_6"));

        var book1 = bookRepository.save(new Book(null, "BookTitle_1", author1, List.of(genre1, genre2)));
        var book2 = bookRepository.save(new Book(null, "BookTitle_2", author2, List.of(genre3, genre4)));
        var book3 = bookRepository.save(new Book(null, "BookTitle_3", author3, List.of(genre5, genre6)));

        commentRepository.save(new Comment(null, "Comment_1_Book_1", book1));
        commentRepository.save(new Comment(null, "Comment_2_Book_1", book1));
        commentRepository.save(new Comment(null, "Comment_1_Book_2", book2));
        commentRepository.save(new Comment(null, "Comment_1_Book_3", book3));
    }
}
