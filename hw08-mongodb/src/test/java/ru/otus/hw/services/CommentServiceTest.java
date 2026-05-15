package ru.otus.hw.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.CommentRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Интеграционный тест сервиса комментариев")
@DataMongoTest
@Import(CommentServiceImpl.class)
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private GenreRepository genreRepository;

    private Book book1;

    private Book book2;

    private Comment comment1;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        genreRepository.deleteAll();

        var author = authorRepository.save(new Author(null, "Author_1"));
        var genre = genreRepository.save(new Genre(null, "Genre_1"));
        book1 = bookRepository.save(new Book(null, "BookTitle_1", author, List.of(genre)));
        book2 = bookRepository.save(new Book(null, "BookTitle_2", author, List.of(genre)));
        comment1 = commentRepository.save(new Comment(null, "Comment_1", book1));
        commentRepository.save(new Comment(null, "Comment_2", book1));
    }

    @DisplayName("должен возвращать комментарий по id")
    @Test
    void findById_shouldReturnComment() {
        var found = commentService.findById(comment1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getText()).isEqualTo("Comment_1");
        assertThat(found.get().getBook().getId()).isEqualTo(book1.getId());
    }

    @DisplayName("должен возвращать пустой Optional для несуществующего id")
    @Test
    void findById_shouldReturnEmptyForNonExistentId() {
        assertThat(commentService.findById("nonexistent-id")).isEmpty();
    }

    @DisplayName("должен возвращать все комментарии книги")
    @Test
    void findByBookId_shouldReturnAllCommentsForBook() {
        var comments = commentService.findByBookId(book1.getId());
        assertThat(comments).hasSize(2)
                .extracting(Comment::getText)
                .containsExactlyInAnyOrder("Comment_1", "Comment_2");
    }

    @DisplayName("должен создавать новый комментарий")
    @Test
    void create_shouldCreateComment() {
        var created = commentService.insert("NewComment", book1.getId());
        assertThat(created.getId()).isNotNull();
        assertThat(created.getText()).isEqualTo("NewComment");
        assertThat(created.getBook().getId()).isEqualTo(book1.getId());
        assertThat(commentRepository.count()).isEqualTo(3);
    }

    @DisplayName("должен выбрасывать исключение при создании комментария для несуществующей книги")
    @Test
    void create_shouldThrowWhenBookNotFound() {
        assertThatThrownBy(() -> commentService.insert("nonexistent-id", "text"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @DisplayName("должен обновлять комментарий")
    @Test
    void update_shouldModifyComment() {
        var updated = commentService.update(comment1.getId(), "UpdatedText", book2.getId());
        assertThat(updated.getText()).isEqualTo("UpdatedText");
        assertThat(updated.getBook().getId()).isEqualTo(book2.getId());
    }

    @DisplayName("должен удалять комментарий")
    @Test
    void deleteById_shouldDeleteComment() {
        commentService.deleteById(comment1.getId());
        assertThat(commentRepository.findById(comment1.getId())).isEmpty();
        assertThat(commentRepository.count()).isEqualTo(1);
    }
}
