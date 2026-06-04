package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookFormDto {

    private String id;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String authorId;

    @NotNull
    @Size(min = 1, message = "At least one genre must be selected")
    private Set<String> genreIds = new HashSet<>();
}
