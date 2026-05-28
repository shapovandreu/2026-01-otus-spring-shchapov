package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.otus.hw.models.Genre;
import ru.otus.hw.services.GenreService;

import static org.mockito.BDDMockito.given;

@DisplayName("GenreRestController")
@WebFluxTest(controllers = {GenreRestController.class})
class GenreRestControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private GenreService genreService;

    @DisplayName("GET /api/genres должен вернуть список жанров")
    @Test
    void listShouldReturnJsonArray() {
        given(genreService.findAll()).willReturn(Flux.just(
                new Genre("1", "Genre_1"),
                new Genre("2", "Genre_2"),
                new Genre("3", "Genre_3")
        ));

        webClient.get().uri("/api/genres")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].id").isEqualTo("1")
                .jsonPath("$[0].name").isEqualTo("Genre_1")
                .jsonPath("$[2].name").isEqualTo("Genre_3");
    }

    @DisplayName("GET /api/genres при пустом списке должен вернуть пустой массив")
    @Test
    void listShouldReturnEmptyArrayWhenNoGenres() {
        given(genreService.findAll()).willReturn(Flux.empty());

        webClient.get().uri("/api/genres")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
