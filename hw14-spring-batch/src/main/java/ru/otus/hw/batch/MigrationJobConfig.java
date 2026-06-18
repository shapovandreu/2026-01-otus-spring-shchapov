package ru.otus.hw.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.processors.AuthorItemProcessor;
import ru.otus.hw.batch.processors.BookItemProcessor;
import ru.otus.hw.batch.processors.CommentItemProcessor;
import ru.otus.hw.batch.processors.GenreItemProcessor;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.mongo.models.GenreDocument;
import ru.otus.hw.relational.models.Author;
import ru.otus.hw.relational.models.Book;
import ru.otus.hw.relational.models.Comment;
import ru.otus.hw.relational.models.Genre;

import java.util.List;

/**
 * Wires the relational -&gt; MongoDB migration job. The job first wipes the target
 * collections, then migrates entities in dependency order (authors and genres,
 * then books that reference them, then comments that reference books) so that all
 * relations stay valid.
 */
@Configuration
@RequiredArgsConstructor
public class MigrationJobConfig {

    public static final String MIGRATION_JOB_NAME = "booksMigrationJob";

    private static final int CHUNK_SIZE = 5;

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    @Bean
    public Step cleanMigrationStep(MongoOperations mongoOperations, IdMappingRegistry idMappingRegistry) {
        return new StepBuilder("cleanMigrationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    List.of("authors", "genres", "books", "comments")
                            .forEach(mongoOperations::dropCollection);
                    idMappingRegistry.clear();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step authorMigrationStep(JpaCursorItemReader<Author> authorReader,
                                    AuthorItemProcessor authorProcessor,
                                    MongoItemWriter<AuthorDocument> authorWriter) {
        return new StepBuilder("authorMigrationStep", jobRepository)
                .<Author, AuthorDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(authorReader)
                .processor(authorProcessor)
                .writer(authorWriter)
                .build();
    }

    @Bean
    public Step genreMigrationStep(JpaCursorItemReader<Genre> genreReader,
                                   GenreItemProcessor genreProcessor,
                                   MongoItemWriter<GenreDocument> genreWriter) {
        return new StepBuilder("genreMigrationStep", jobRepository)
                .<Genre, GenreDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(genreReader)
                .processor(genreProcessor)
                .writer(genreWriter)
                .build();
    }

    @Bean
    public Step bookMigrationStep(JpaCursorItemReader<Book> bookReader,
                                  BookItemProcessor bookProcessor,
                                  MongoItemWriter<BookDocument> bookWriter) {
        return new StepBuilder("bookMigrationStep", jobRepository)
                .<Book, BookDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(bookReader)
                .processor(bookProcessor)
                .writer(bookWriter)
                .build();
    }

    @Bean
    public Step commentMigrationStep(JpaCursorItemReader<Comment> commentReader,
                                     CommentItemProcessor commentProcessor,
                                     MongoItemWriter<CommentDocument> commentWriter) {
        return new StepBuilder("commentMigrationStep", jobRepository)
                .<Comment, CommentDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(commentReader)
                .processor(commentProcessor)
                .writer(commentWriter)
                .build();
    }

    @Bean
    public Job booksMigrationJob(Step cleanMigrationStep,
                                 Step authorMigrationStep,
                                 Step genreMigrationStep,
                                 Step bookMigrationStep,
                                 Step commentMigrationStep) {
        return new JobBuilder(MIGRATION_JOB_NAME, jobRepository)
                .start(cleanMigrationStep)
                .next(authorMigrationStep)
                .next(genreMigrationStep)
                .next(bookMigrationStep)
                .next(commentMigrationStep)
                .build();
    }
}
