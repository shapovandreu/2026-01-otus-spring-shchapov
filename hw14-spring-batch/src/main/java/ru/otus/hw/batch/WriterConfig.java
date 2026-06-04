package ru.otus.hw.batch;

import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.mongo.models.GenreDocument;

/**
 * Writers into the MongoDB (target) store. Each writer upserts documents into its
 * collection; {@code @DocumentReference} fields are stored as id references, so the
 * relations are kept on the NoSQL side as well.
 */
@Configuration
public class WriterConfig {

    @Bean
    public MongoItemWriter<AuthorDocument> authorWriter(MongoOperations mongoOperations) {
        return new MongoItemWriterBuilder<AuthorDocument>()
                .template(mongoOperations)
                .collection("authors")
                .build();
    }

    @Bean
    public MongoItemWriter<GenreDocument> genreWriter(MongoOperations mongoOperations) {
        return new MongoItemWriterBuilder<GenreDocument>()
                .template(mongoOperations)
                .collection("genres")
                .build();
    }

    @Bean
    public MongoItemWriter<BookDocument> bookWriter(MongoOperations mongoOperations) {
        return new MongoItemWriterBuilder<BookDocument>()
                .template(mongoOperations)
                .collection("books")
                .build();
    }

    @Bean
    public MongoItemWriter<CommentDocument> commentWriter(MongoOperations mongoOperations) {
        return new MongoItemWriterBuilder<CommentDocument>()
                .template(mongoOperations)
                .collection("comments")
                .build();
    }
}
