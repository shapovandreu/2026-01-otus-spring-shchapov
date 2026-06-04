package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.otus.hw.models.Comment;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private String id;

    private String text;

    public static CommentDto fromDomain(Comment comment) {
        return new CommentDto(comment.getId(), comment.getText());
    }
}
