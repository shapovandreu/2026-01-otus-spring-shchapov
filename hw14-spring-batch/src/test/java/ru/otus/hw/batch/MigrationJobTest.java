package ru.otus.hw.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
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

        var book = mongoOperations.findById("1", BookDocument.class);
        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("BookTitle_1");
        assertThat(book.getAuthor().getId()).isEqualTo("1");
        assertThat(book.getAuthor().getFullName()).isEqualTo("Author_1");
        assertThat(book.getGenres())
                .extracting(GenreDocument::getId)
                .containsExactlyInAnyOrder("1", "2");

        var comment = mongoOperations.findById("1", CommentDocument.class);
        assertThat(comment).isNotNull();
        assertThat(comment.getBook().getId()).isEqualTo("1");
    }
}
