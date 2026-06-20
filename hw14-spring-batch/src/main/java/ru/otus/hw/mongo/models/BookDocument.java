package ru.otus.hw.mongo.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"author", "genres"})
@ToString(exclude = {"author", "genres"})
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "books")
public class BookDocument {

    @Id
    private String id;

    private String title;

    @DocumentReference
    private AuthorDocument author;

    @DocumentReference
    private List<GenreDocument> genres;
}
