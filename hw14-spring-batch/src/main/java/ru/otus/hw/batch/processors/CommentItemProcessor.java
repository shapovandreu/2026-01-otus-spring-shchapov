package ru.otus.hw.batch.processors;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.relational.models.Comment;

/**
 * Converts a relational {@link Comment} into its MongoDB counterpart,
 * keeping the reference to the owning book by its preserved id.
 */
@Component
public class CommentItemProcessor implements ItemProcessor<Comment, CommentDocument> {

    @Override
    public CommentDocument process(Comment comment) {
        var book = new BookDocument();
        book.setId(String.valueOf(comment.getBook().getId()));
        return new CommentDocument(String.valueOf(comment.getId()), comment.getText(), book);
    }
}
