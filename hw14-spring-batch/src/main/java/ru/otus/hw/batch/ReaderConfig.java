package ru.otus.hw.batch;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.otus.hw.relational.models.Author;
import ru.otus.hw.relational.models.Book;
import ru.otus.hw.relational.models.Comment;
import ru.otus.hw.relational.models.Genre;

/**
 * Readers over the relational (source) store. {@link JpaCursorItemReader} keeps a
 * single {@code EntityManager} open while iterating, which lets lazy associations
 * (book author/genres, comment book) be resolved inside the processors.
 */
@Configuration
public class ReaderConfig {

    @Bean
    public JpaCursorItemReader<Author> authorReader(EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<Author>()
                .name("authorReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select a from Author a")
                .build();
    }

    @Bean
    public JpaCursorItemReader<Genre> genreReader(EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<Genre>()
                .name("genreReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select g from Genre g")
                .build();
    }

    @Bean
    public JpaCursorItemReader<Book> bookReader(EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<Book>()
                .name("bookReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select b from Book b")
                .build();
    }

    @Bean
    public JpaCursorItemReader<Comment> commentReader(EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<Comment>()
                .name("commentReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select c from Comment c")
                .build();
    }
}
