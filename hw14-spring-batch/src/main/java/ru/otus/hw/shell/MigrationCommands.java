package ru.otus.hw.shell;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.mongo.models.AuthorDocument;
import ru.otus.hw.mongo.models.BookDocument;
import ru.otus.hw.mongo.models.CommentDocument;
import ru.otus.hw.mongo.models.GenreDocument;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class MigrationCommands {

    private final JobLauncher jobLauncher;

    private final Job booksMigrationJob;

    private final MongoOperations mongoOperations;

    /**
     * Runs the migration with stable (empty) job parameters. If a previous run of this
     * instance failed, the same command restarts it from the failed step; if it already
     * completed, Spring Batch refuses to run it again and the reason is reported.
     */
    @ShellMethod(value = "Start (or restart) the relational -> MongoDB migration", key = {"migrate", "m"})
    public String migrate() {
        return launch(new JobParameters());
    }

    /**
     * Starts a brand-new migration instance (unique parameters), wiping the target
     * collections and migrating everything from scratch.
     */
    @ShellMethod(value = "Start a fresh migration as a new job instance", key = {"migrate-fresh", "mf"})
    public String migrateFresh() {
        var parameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        return launch(parameters);
    }

    @ShellMethod(value = "Show document counts in the target MongoDB", key = {"migrated", "stats"})
    public String showMigrated() {
        return """
                authors:  %d
                genres:   %d
                books:    %d
                comments: %d""".formatted(
                mongoOperations.count(new Query(), AuthorDocument.class),
                mongoOperations.count(new Query(), GenreDocument.class),
                mongoOperations.count(new Query(), BookDocument.class),
                mongoOperations.count(new Query(), CommentDocument.class));
    }

    private String launch(JobParameters parameters) {
        try {
            JobExecution execution = jobLauncher.run(booksMigrationJob, parameters);
            return "Migration job finished with status: " + execution.getStatus();
        } catch (JobInstanceAlreadyCompleteException e) {
            return "This migration instance has already completed. Use 'migrate-fresh' to run a new one.";
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
            return "Could not launch migration job: " + e.getMessage();
        }
    }
}
