package ru.otus.hw.batch.processors;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.GenreDocument;
import ru.otus.hw.relational.models.Book;

import java.util.List;

/**
 * Converts a relational {@link Book} into its MongoDB counterpart.
 * The author and the genres are referenced by their (preserved) ids, so the
 * relations established in the relational store survive the migration.
 */
@Component
public class BookItemProcessor implements ItemProcessor<Book, BookDocument> {

    @Override
    public BookDocument process(Book book) {
        var author = new AuthorDocument(String.valueOf(book.getAuthor().getId()),
                book.getAuthor().getFullName());

        List<GenreDocument> genres = book.getGenres().stream()
                .map(genre -> new GenreDocument(String.valueOf(genre.getId()), genre.getName()))
                .toList();

        return new BookDocument(String.valueOf(book.getId()), book.getTitle(), author, genres);
    }
}
