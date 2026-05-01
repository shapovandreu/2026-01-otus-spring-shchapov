package ru.otus.hw.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.models.Comment;
import ru.otus.hw.repositories.JpaBookRepository;
import ru.otus.hw.repositories.JpaCommentRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Интеграционный тест сервиса комментариев (проверка ленивой загрузки связей)")
@DataJpaTest
@Import({CommentServiceImpl.class, JpaCommentRepository.class, JpaBookRepository.class})
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @DisplayName("должен загружать комментарий со связями без LazyInitializationException при findById")
    @Test
    void shouldLoadCommentWithBookWhenFindById() {
        var commentOpt = commentService.findById(1L);
        assertThat(commentOpt).isPresent();
        Comment comment = commentOpt.get();

        assertNotNull(comment.getBook());
        assertDoesNotThrow(() -> comment.getBook().getTitle());
    }

    @DisplayName("должен загружать комментарии со связями без LazyInitializationException при findByBookId")
    @Test
    void shouldLoadCommentsWithBookWhenFindByBookId() {
        List<Comment> comments = commentService.findByBookId(1L);
        assertThat(comments).isNotEmpty();

        for (Comment comment : comments) {
            assertNotNull(comment.getBook());
            assertDoesNotThrow(() -> comment.getBook().getTitle());
        }
    }

    @DisplayName("должен вернуть созданный комментарий с доступной книгой без LazyInitializationException")
    @Test
    void shouldReturnCreatedCommentWithBookLoaded() {
        Comment created = commentService.create(1L, "Новый комментарий");
        assertNotNull(created.getBook());
        assertDoesNotThrow(() -> created.getBook().getTitle());
        assertThat(created.getBook().getId()).isEqualTo(1L);
    }

    @DisplayName("должен вернуть обновленный комментарий с доступной книгой без LazyInitializationException")
    @Test
    void shouldReturnUpdatedCommentWithBookLoaded() {
        Comment updated = commentService.update(1L, "Обновленный текст", 2L);
        assertNotNull(updated.getBook());
        assertDoesNotThrow(() -> updated.getBook().getTitle());
        assertThat(updated.getBook().getId()).isEqualTo(2L);
    }

}