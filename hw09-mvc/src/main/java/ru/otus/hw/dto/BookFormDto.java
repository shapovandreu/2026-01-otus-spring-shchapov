package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.otus.hw.models.Book;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookFormDto {

    private long id;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    private Long authorId;

    @NotNull
    @Size(min = 1, message = "At least one genre must be selected")
    private Set<Long> genreIds = new HashSet<>();

    public static BookFormDto fromDomain(Book book) {
        var genreIds = book.getGenres().stream()
                .map(g -> g.getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        return new BookFormDto(book.getId(), book.getTitle(), book.getAuthor().getId(), genreIds);
    }
}
