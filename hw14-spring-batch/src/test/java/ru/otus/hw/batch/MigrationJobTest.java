package ru.otus.hw.batch;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.mongo.models.GenreDocument;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.shell.interactive.enabled=false")
@SpringBatchTest
class MigrationJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private MongoOperations mongoOperations;

    @Test
    void shouldMigrateAllEntitiesPreservingRelations() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(mongoOperations.count(new Query(), AuthorDocument.class)).isEqualTo(3);
        assertThat(mongoOperations.count(new Query(), GenreDocument.class)).isEqualTo(6);
        assertThat(mongoOperations.count(new Query(), BookDocument.class)).isEqualTo(3);
        assertThat(mongoOperations.count(new Query(), CommentDocument.class)).isEqualTo(4);

        var book = mongoOperations.findOne(
                Query.query(Criteria.where("title").is("BookTitle_1")), BookDocument.class);
        assertThat(book).isNotNull();
        // The Mongo store owns its id space: the id must be a freshly generated
        // ObjectId, not the reused relational id.
        assertThat(ObjectId.isValid(book.getId())).isTrue();

        // References resolve through the new Mongo ids, so the relations survive.
        assertThat(book.getAuthor()).isNotNull();
        assertThat(book.getAuthor().getFullName()).isEqualTo("Author_1");
        assertThat(book.getGenres())
                .extracting(GenreDocument::getName)
                .containsExactlyInAnyOrder("Genre_1", "Genre_2");

        var comment = mongoOperations.findOne(
                Query.query(Criteria.where("text").is("Comment_1_Book_1")), CommentDocument.class);
        assertThat(comment).isNotNull();
        assertThat(comment.getBook()).isNotNull();
        assertThat(comment.getBook().getId()).isEqualTo(book.getId());
    }
}
