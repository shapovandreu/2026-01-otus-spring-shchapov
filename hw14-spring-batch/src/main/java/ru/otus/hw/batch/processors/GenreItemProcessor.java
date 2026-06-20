package ru.otus.hw.batch.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.batch.IdMappingRegistry;
import ru.otus.hw.mongo.models.GenreDocument;
import ru.otus.hw.relational.models.Genre;

@Component
@RequiredArgsConstructor
public class GenreItemProcessor implements ItemProcessor<Genre, GenreDocument> {

    private final IdMappingRegistry idMappingRegistry;

    @Override
    public GenreDocument process(Genre genre) {
        String targetId = idMappingRegistry.mapGenre(genre.getId());
        return new GenreDocument(targetId, genre.getName());
    }
}
