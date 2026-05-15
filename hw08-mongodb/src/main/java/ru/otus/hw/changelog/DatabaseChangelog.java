package ru.otus.hw.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

@ChangeLog
public class DatabaseChangelog {

    @ChangeSet(order = "001", id = "dropDb", author = "shchapov", runAlways = true)
    public void dropDb(MongoTemplate mongoTemplate) {
        mongoTemplate.getMongoDatabaseFactory().getMongoDatabase().drop();
    }

    @ChangeSet(order = "002", id = "insertAuthors", author = "shchapov")
    public void insertAuthors(MongoTemplate mongoTemplate) {
        var authorDoc1 = new Document().append("_id", "1").append("fullName", "Author_1");
        var authorDoc2 = new Document().append("_id", "2").append("fullName", "Author_2");
        var authorDoc3 = new Document().append("_id", "3").append("fullName", "Author_3");
        mongoTemplate.insert(List.of(authorDoc1, authorDoc2, authorDoc3), "authors");
    }

    @ChangeSet(order = "003", id = "insertGenres", author = "shchapov")
    public void insertGenres(MongoTemplate mongoTemplate) {
        var genreDoc1 = new Document().append("_id", "1").append("name", "Genre_1");
        var genreDoc2 = new Document().append("_id", "2").append("name", "Genre_2");
        var genreDoc3 = new Document().append("_id", "3").append("name", "Genre_3");
        var genreDoc4 = new Document().append("_id", "4").append("name", "Genre_4");
        var genreDoc5 = new Document().append("_id", "5").append("name", "Genre_5");
        var genreDoc6 = new Document().append("_id", "6").append("name", "Genre_6");
        mongoTemplate.insert(List.of(genreDoc1, genreDoc2, genreDoc3, genreDoc4, genreDoc5, genreDoc6), "genres");
    }

    @ChangeSet(order = "004", id = "insertBooks", author = "shchapov")
    public void insertBooks(MongoTemplate mongoTemplate) {
        var bookDoc1 = new Document().append("_id", "1")
                .append("title", "BookTitle_1")
                .append("author", "1")
                .append("genres", List.of("1", "2"));
        var bookDoc2 = new Document().append("_id", "2")
                .append("title", "BookTitle_2")
                .append("author", "2")
                .append("genres", List.of("3", "4"));
        var bookDoc3 = new Document().append("_id", "3")
                .append("title", "BookTitle_3")
                .append("author", "3")
                .append("genres", List.of("5", "6"));
        mongoTemplate.insert(List.of(bookDoc1,  bookDoc2, bookDoc3), "books");
    }

    @ChangeSet(order = "005", id = "insertComments", author = "shchapov")
    public void insertComments(MongoTemplate mongoTemplate) {
        var commentDoc1 = new Document().append("_id", "1")
                .append("text", "Comment_1")
                .append("book", "1");
        var commentDoc2 = new Document().append("_id", "2")
                .append("text", "Comment_2")
                .append("book", "2");
        var commentDoc3 = new Document().append("_id", "3")
                .append("text", "Comment_3")
                .append("book", "3");
        mongoTemplate.insert(List.of(commentDoc1,  commentDoc2, commentDoc3), "comments");
    }
}