package ru.otus.hw.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест сервиса книг")
@DataMongoTest
@Import(BookServiceImpl.class)
class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Author author1;

    private Author author2;

    private Genre genre1;

    private Genre genre2;

    private Genre genre3;

    private Genre genre4;

    private Book book1;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        genreRepository.deleteAll();

        author1 = authorRepository.save(new Author(null, "Author_1"));
        author2 = authorRepository.save(new Author(null, "Author_2"));
        genre1 = genreRepository.save(new Genre(null, "Genre_1"));
        genre2 = genreRepository.save(new Genre(null, "Genre_2"));
        genre3 = genreRepository.save(new Genre(null, "Genre_3"));
        genre4 = genreRepository.save(new Genre(null, "Genre_4"));
        book1 = bookRepository.save(new Book(null, "BookTitle_1", author1, List.of(genre1, genre2)));
        bookRepository.save(new Book(null, "BookTitle_2", author2, List.of(genre3, genre4)));
    }

    @DisplayName("должен возвращать все книги")
    @Test
    void findAll_shouldReturnAllBooks() {
        var books = bookService.findAll();
        assertThat(books).hasSize(2)
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("BookTitle_1", "BookTitle_2");
    }

    @DisplayName("должен возвращать книгу по id с встроенными автором и жанрами")
    @Test
    void findById_shouldReturnBookWithAuthorAndGenres() {
        var found = bookService.findById(book1.getId());
        assertThat(found).isPresent();
        var book = found.get();
        assertThat(book.getTitle()).isEqualTo("BookTitle_1");
        assertThat(book.getAuthor()).isNotNull();
        assertThat(book.getAuthor().getFullName()).isEqualTo("Author_1");
        assertThat(book.getGenres()).hasSize(2);
    }

    @DisplayName("должен возвращать пустой Optional для несуществующего id")
    @Test
    void findById_shouldReturnEmptyForNonExistentId() {
        var found = bookService.findById("nonexistent-id");
        assertThat(found).isEmpty();
    }

    @DisplayName("должен создавать новую книгу")
    @Test
    void insert_shouldCreateNewBook() {
        var genreIds = Set.of(genre1.getId(), genre2.getId());
        var saved = bookService.insert("NewBook", author1.getId(), genreIds);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("NewBook");
        assertThat(saved.getAuthor().getFullName()).isEqualTo("Author_1");
        assertThat(saved.getGenres()).hasSize(2);
        assertThat(bookRepository.count()).isEqualTo(3);
    }

    @DisplayName("должен обновлять книгу")
    @Test
    void update_shouldModifyExistingBook() {
        var genreIds = Set.of(genre3.getId(), genre4.getId());
        var updated = bookService.update(book1.getId(), "UpdatedTitle", author2.getId(), genreIds);

        assertThat(updated.getId()).isEqualTo(book1.getId());
        assertThat(updated.getTitle()).isEqualTo("UpdatedTitle");
        assertThat(updated.getAuthor().getFullName()).isEqualTo("Author_2");
        assertThat(updated.getGenres()).hasSize(2);
    }

    @DisplayName("должен удалять книгу и её комментарии")
    @Test
    void deleteById_shouldDeleteBookAndItsComments() {
        commentRepository.save(new Comment(null, "Comment_1", book1));
        commentRepository.save(new Comment(null, "Comment_2", book1));

        bookService.deleteById(book1.getId());

        assertThat(bookRepository.findById(book1.getId())).isEmpty();
        assertThat(commentRepository.findByBookId(book1.getId())).isEmpty();
    }
}
