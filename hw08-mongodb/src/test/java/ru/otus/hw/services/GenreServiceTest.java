package ru.otus.hw.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест сервиса жанров")
@DataMongoTest
@Import(GenreServiceImpl.class)
class GenreServiceTest {

    @Autowired
    private GenreService genreService;

    @Autowired
    private GenreRepository genreRepository;

    @BeforeEach
    void setUp() {
        genreRepository.deleteAll();
        genreRepository.saveAll(List.of(
                new Genre(null, "Genre_1"),
                new Genre(null, "Genre_2"),
                new Genre(null, "Genre_3"),
                new Genre(null, "Genre_4"),
                new Genre(null, "Genre_5"),
                new Genre(null, "Genre_6")
        ));
    }

    @DisplayName("должен возвращать все жанры")
    @Test
    void findAll_shouldReturnAllGenres() {
        var genres = genreService.findAll();
        assertThat(genres).hasSize(6)
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder(
                        "Genre_1", "Genre_2", "Genre_3", "Genre_4", "Genre_5", "Genre_6");
    }
}
