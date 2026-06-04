package ru.otus.hw.batch.processors;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.mongo.models.GenreDocument;
import ru.otus.hw.relational.models.Genre;

/**
 * Converts a relational {@link Genre} into its MongoDB counterpart,
 * preserving the original id as a string.
 */
@Component
public class GenreItemProcessor implements ItemProcessor<Genre, GenreDocument> {

    @Override
    public GenreDocument process(Genre genre) {
        return new GenreDocument(String.valueOf(genre.getId()), genre.getName());
    }
}
