package ru.otus.hw.converters;

import org.springframework.stereotype.Component;
import ru.otus.hw.models.Comment;

@Component
public class CommentConverter {

    public String commentToString(Comment comment) {
        // getBook().getId() не вызывает загрузку книги из базы, так как идентификатор
        // доступен через прокси без инициализации сущности.
        long bookId = comment.getBook() != null ? comment.getBook().getId() : 0;
        return "Id: %d, text: %s, bookId: %d".formatted(comment.getId(), comment.getText(), bookId);
    }
}