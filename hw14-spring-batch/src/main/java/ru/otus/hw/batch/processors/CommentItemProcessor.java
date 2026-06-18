package ru.otus.hw.batch.processors;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.batch.IdMappingRegistry;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.relational.models.Comment;

@Component
@RequiredArgsConstructor
public class CommentItemProcessor implements ItemProcessor<Comment, CommentDocument> {

    private final IdMappingRegistry idMappingRegistry;

    @Override
    public CommentDocument process(Comment comment) {
        var book = new BookDocument();
        book.setId(idMappingRegistry.resolveBook(comment.getBook().getId()));
        return new CommentDocument(new ObjectId().toString(), comment.getText(), book);
    }
}
