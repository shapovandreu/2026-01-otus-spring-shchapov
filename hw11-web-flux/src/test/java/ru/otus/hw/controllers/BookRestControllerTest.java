package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("BookRestController")
@WebFluxTest(controllers = {BookRestController.class, RestExceptionHandler.class})
class BookRestControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private CommentService commentService;

    private Book sampleBook() {
        var author = new Author("1", "Author_1");
        var genre1 = new Genre("1", "Genre_1");
        var genre2 = new Genre("2", "Genre_2");
        return new Book("1", "BookTitle_1", author, List.of(genre1, genre2));
    }

    @DisplayName("GET /api/books должен вернуть список книг")
    @Test
    void listShouldReturnJsonArray() {
        given(bookService.findAll()).willReturn(Flux.just(sampleBook()));

        webClient.get().uri("/api/books")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("1")
                .jsonPath("$[0].title").isEqualTo("BookTitle_1")
                .jsonPath("$[0].author.fullName").isEqualTo("Author_1");
    }

    @DisplayName("GET /api/books/{id} должен вернуть книгу")
    @Test
    void getByIdShouldReturnBook() {
        given(bookService.findById("1")).willReturn(Mono.just(sampleBook()));

        webClient.get().uri("/api/books/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.title").isEqualTo("BookTitle_1")
                .jsonPath("$.genres.length()").isEqualTo(2);
    }

    @DisplayName("GET /api/books/{id} должен вернуть 404 если книга не найдена")
    @Test
    void getByIdShouldReturn404WhenNotFound() {
        given(bookService.findById("99")).willReturn(Mono.empty());

        webClient.get().uri("/api/books/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @DisplayName("POST /api/books должен создать книгу и вернуть 201")
    @Test
    void createShouldReturn201WithBook() {
        given(bookService.insert(anyString(), anyString(), any())).willReturn(Mono.just(sampleBook()));

        var body = Map.of("title", "NewBook", "authorId", "1", "genreIds", List.of("1", "2"));

        webClient.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.title").isEqualTo("BookTitle_1");

        verify(bookService).insert(eq("NewBook"), eq("1"), eq(Set.of("1", "2")));
    }

    @DisplayName("POST /api/books с пустым заголовком должен вернуть 400")
    @Test
    void createWithBlankTitleShouldReturn400() {
        var body = Map.of("title", "", "authorId", "1", "genreIds", List.of("1"));

        webClient.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").exists();

        verify(bookService, never()).insert(anyString(), anyString(), any());
    }

    @DisplayName("PUT /api/books/{id} должен обновить книгу")
    @Test
    void updateShouldReturnUpdatedBook() {
        given(bookService.update(anyString(), anyString(), anyString(), any()))
                .willReturn(Mono.just(sampleBook()));

        var body = Map.of("title", "Updated", "authorId", "2", "genreIds", List.of("3", "4"));

        webClient.put().uri("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1");

        verify(bookService).update(eq("1"), eq("Updated"), eq("2"), eq(Set.of("3", "4")));
    }

    @DisplayName("PUT /api/books/{id} с пустым заголовком должен вернуть 400")
    @Test
    void updateWithBlankTitleShouldReturn400() {
        var body = Map.of("title", "", "authorId", "1", "genreIds", List.of("1"));

        webClient.put().uri("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").exists();

        verify(bookService, never()).update(anyString(), anyString(), anyString(), any());
    }

    @DisplayName("PUT /api/books/{id} когда книга не найдена должен вернуть 404")
    @Test
    void updateShouldReturn404WhenBookMissing() {
        given(bookService.update(anyString(), anyString(), anyString(), any()))
                .willReturn(Mono.error(new EntityNotFoundException("Book with id 99 not found")));

        var body = Map.of("title", "Title", "authorId", "1", "genreIds", List.of("1"));

        webClient.put().uri("/api/books/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @DisplayName("DELETE /api/books/{id} должен удалить книгу и вернуть 204")
    @Test
    void deleteShouldReturn204() {
        given(bookService.deleteById("1")).willReturn(Mono.empty());

        webClient.delete().uri("/api/books/1")
                .exchange()
                .expectStatus().isNoContent();

        verify(bookService).deleteById("1");
    }

    @DisplayName("GET /api/books/{id}/comments должен вернуть список комментариев")
    @Test
    void getCommentsShouldReturnJsonList() {
        given(commentService.findByBookId("1")).willReturn(Flux.just(
                new Comment("10", "Great book", "1"),
                new Comment("11", "Loved it", "1")
        ));

        webClient.get().uri("/api/books/1/comments")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].text").isEqualTo("Great book")
                .jsonPath("$[1].text").isEqualTo("Loved it");
    }
}
