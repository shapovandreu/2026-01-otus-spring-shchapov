package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.otus.hw.models.Genre;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreDto {

    private long id;

    private String name;

    public static GenreDto fromDomain(Genre genre) {
        return new GenreDto(genre.getId(), genre.getName());
    }
}
