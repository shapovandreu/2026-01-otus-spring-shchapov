package ru.otus.hw.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.UserRepository;
import ru.otus.hw.security.SecurityConfig;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;
import ru.otus.hw.services.CustomUserDetailsService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookRestController")
@WebMvcTest(controllers = {BookRestController.class, RestExceptionHandler.class})
@Import({SecurityConfig.class, CustomUserDetailsService.class})
@WithMockUser
class BookRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private UserRepository userRepository;

    private Book sampleBook() {
        var author = new Author(1L, "Author_1");
        var genre1 = new Genre(1L, "Genre_1");
        var genre2 = new Genre(2L, "Genre_2");
        return new Book(1L, "BookTitle_1", author, List.of(genre1, genre2));
    }

    @DisplayName("GET /api/books должен вернуть список книг")
    @Test
    void listShouldReturnJsonArray() throws Exception {
        given(bookService.findAll()).willReturn(List.of(sampleBook()));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(1)))
                .andExpect(jsonPath("$[0].title", equalTo("BookTitle_1")))
                .andExpect(jsonPath("$[0].author.fullName", equalTo("Author_1")));
    }

    @DisplayName("GET /api/books/{id} должен вернуть книгу")
    @Test
    void getByIdShouldReturnBook() throws Exception {
        given(bookService.findById(1L)).willReturn(Optional.of(sampleBook()));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.title", equalTo("BookTitle_1")))
                .andExpect(jsonPath("$.genres", hasSize(2)));
    }

    @DisplayName("GET /api/books/{id} должен вернуть 404 если книга не найдена")
    @Test
    void getByIdShouldReturn404WhenNotFound() throws Exception {
        given(bookService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @DisplayName("POST /api/books должен создать книгу и вернуть 201")
    @Test
    void createShouldReturn201WithBook() throws Exception {
        given(bookService.insert(anyString(), anyLong(), any())).willReturn(sampleBook());

        var body = Map.of("title", "NewBook", "authorId", 1, "genreIds", List.of(1, 2));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.title", equalTo("BookTitle_1")));

        verify(bookService).insert(eq("NewBook"), eq(1L), eq(Set.of(1L, 2L)));
    }

    @DisplayName("POST /api/books с пустым заголовком должен вернуть 400")
    @Test
    void createWithBlankTitleShouldReturn400() throws Exception {
        var body = Map.of("title", "", "authorId", 1, "genreIds", List.of(1));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());

        verify(bookService, never()).insert(anyString(), anyLong(), any());
    }

    @DisplayName("PUT /api/books/{id} должен обновить книгу")
    @Test
    void updateShouldReturnUpdatedBook() throws Exception {
        given(bookService.update(anyLong(), anyString(), anyLong(), any())).willReturn(sampleBook());

        var body = Map.of("title", "Updated", "authorId", 2, "genreIds", List.of(3, 4));

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)));

        verify(bookService).update(eq(1L), eq("Updated"), eq(2L), eq(Set.of(3L, 4L)));
    }

    @DisplayName("PUT /api/books/{id} с пустым заголовком должен вернуть 400")
    @Test
    void updateWithBlankTitleShouldReturn400() throws Exception {
        var body = Map.of("title", "", "authorId", 1, "genreIds", List.of(1));

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());

        verify(bookService, never()).update(anyLong(), anyString(), anyLong(), any());
    }

    @DisplayName("PUT /api/books/{id} когда книга не найдена должен вернуть 404")
    @Test
    void updateShouldReturn404WhenBookMissing() throws Exception {
        given(bookService.update(anyLong(), anyString(), anyLong(), any()))
                .willThrow(new ru.otus.hw.exceptions.EntityNotFoundException("Book with id 99 not found"));

        var body = Map.of("title", "Title", "authorId", 1, "genreIds", List.of(1));

        mockMvc.perform(put("/api/books/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @DisplayName("DELETE /api/books/{id} должен удалить книгу и вернуть 204")
    @Test
    void deleteShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).deleteById(1L);
    }

    @DisplayName("GET /api/books/{id}/comments должен вернуть список комментариев")
    @Test
    void getCommentsShouldReturnJsonList() throws Exception {
        var book = sampleBook();
        given(commentService.findByBookId(1L))
                .willReturn(List.of(
                        new Comment(10L, "Great book", book),
                        new Comment(11L, "Loved it", book)
                ));

        mockMvc.perform(get("/api/books/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].text", containsInAnyOrder("Great book", "Loved it")));
    }
}
