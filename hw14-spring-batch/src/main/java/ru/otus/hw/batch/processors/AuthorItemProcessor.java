package ru.otus.hw.batch.processors;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.relational.models.Author;

/**
 * Converts a relational {@link Author} into its MongoDB counterpart.
 * The original numeric id is preserved as a string so that references
 * from books stay consistent after the migration.
 */
@Component
public class AuthorItemProcessor implements ItemProcessor<Author, AuthorDocument> {

    @Override
    public AuthorDocument process(Author author) {
        return new AuthorDocument(String.valueOf(author.getId()), author.getFullName());
    }
}
