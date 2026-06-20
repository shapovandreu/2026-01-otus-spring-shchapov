package ru.otus.hw.batch.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.batch.IdMappingRegistry;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.relational.models.Author;

@Component
@RequiredArgsConstructor
public class AuthorItemProcessor implements ItemProcessor<Author, AuthorDocument> {

    private final IdMappingRegistry idMappingRegistry;

    @Override
    public AuthorDocument process(Author author) {
        String targetId = idMappingRegistry.mapAuthor(author.getId());
        return new AuthorDocument(targetId, author.getFullName());
    }
}
