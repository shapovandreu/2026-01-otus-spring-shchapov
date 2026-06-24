package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.otus.hw.models.Book;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {

    private long id;

    private String title;

    private AuthorDto author;

    private List<GenreDto> genres;

    public static BookDto fromDomain(Book book) {
        var genres = book.getGenres().stream()
                .map(GenreDto::fromDomain)
                .toList();
        return new BookDto(book.getId(), book.getTitle(), AuthorDto.fromDomain(book.getAuthor()), genres);
    }
}
