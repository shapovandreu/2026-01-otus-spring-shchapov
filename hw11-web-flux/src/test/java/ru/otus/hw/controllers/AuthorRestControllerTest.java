package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.otus.hw.models.Author;
import ru.otus.hw.services.AuthorService;

import static org.mockito.BDDMockito.given;

@DisplayName("AuthorRestController")
@WebFluxTest(controllers = {AuthorRestController.class})
class AuthorRestControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private AuthorService authorService;

    @DisplayName("GET /api/authors должен вернуть список авторов")
    @Test
    void listShouldReturnJsonArray() {
        given(authorService.findAll()).willReturn(Flux.just(
                new Author("1", "Author_1"),
                new Author("2", "Author_2")
        ));

        webClient.get().uri("/api/authors")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("1")
                .jsonPath("$[0].fullName").isEqualTo("Author_1")
                .jsonPath("$[1].fullName").isEqualTo("Author_2");
    }

    @DisplayName("GET /api/authors при пустом списке должен вернуть пустой массив")
    @Test
    void listShouldReturnEmptyArrayWhenNoAuthors() {
        given(authorService.findAll()).willReturn(Flux.empty());

        webClient.get().uri("/api/authors")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
