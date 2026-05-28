package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.otus.hw.models.Author;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDto {

    private long id;

    private String fullName;

    public static AuthorDto fromDomain(Author author) {
        return new AuthorDto(author.getId(), author.getFullName());
    }
}
