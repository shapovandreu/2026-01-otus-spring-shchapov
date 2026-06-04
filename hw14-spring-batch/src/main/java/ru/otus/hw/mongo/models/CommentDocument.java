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

@Getter
@Setter
@EqualsAndHashCode(exclude = "book")
@ToString(exclude = "book")
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "comments")
public class CommentDocument {

    @Id
    private String id;

    private String text;

    @DocumentReference
    private BookDocument book;
}
