package ru.otus.hw.batch.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.batch.IdMappingRegistry;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.GenreDocument;
import ru.otus.hw.relational.models.Book;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookItemProcessor implements ItemProcessor<Book, BookDocument> {

    private final IdMappingRegistry idMappingRegistry;

    @Override
    public BookDocument process(Book book) {
        String targetId = idMappingRegistry.mapBook(book.getId());

        var author = new AuthorDocument();
        author.setId(idMappingRegistry.resolveAuthor(book.getAuthor().getId()));

        List<GenreDocument> genres = book.getGenres().stream()
                .map(genre -> {
                    var genreDocument = new GenreDocument();
                    genreDocument.setId(idMappingRegistry.resolveGenre(genre.getId()));
                    return genreDocument;
                })
                .toList();

        return new BookDocument(targetId, book.getTitle(), author, genres);
    }
}
