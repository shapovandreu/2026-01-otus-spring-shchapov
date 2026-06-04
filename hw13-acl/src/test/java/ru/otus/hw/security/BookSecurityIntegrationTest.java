package ru.otus.hw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Авторизация контроллеров (URL + доменные сущности через ACL)")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String bookForm(long authorId, List<Long> genreIds) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("title", "Some title", "authorId", authorId, "genreIds", genreIds));
    }

    // ---------- URL-level authorization: anonymous access is denied ----------

    @DisplayName("аноним перенаправляется на /login при доступе к защищённым страницам")
    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        for (var url : List.of("/", "/books", "/books/1", "/authors", "/genres")) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @DisplayName("аноним перенаправляется на /login при доступе к REST API")
    @Test
    void anonymousIsRedirectedOnApi() throws Exception {
        for (var url : List.of("/api/books", "/api/books/1", "/api/authors", "/api/genres")) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @DisplayName("страница /login доступна без аутентификации")
    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // ---------- Authenticated USER: domain-entity (ACL) authorization ----------

    @DisplayName("USER видит только книги, на которые у него есть право READ")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userSeesOnlyReadableBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == 3)]", hasSize(0)));
    }

    @DisplayName("USER может открыть книгу с правом READ")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCanReadPermittedBook() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/books/2"))
                .andExpect(status().isOk());
    }

    @DisplayName("USER получает 403 при чтении книги без права READ")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCannotReadForbiddenBook() throws Exception {
        mockMvc.perform(get("/api/books/3"))
                .andExpect(status().isForbidden());
    }

    @DisplayName("USER может изменить книгу с правом WRITE")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCanUpdateWritableBook() throws Exception {
        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookForm(1, List.of(1L, 2L))))
                .andExpect(status().isOk());
    }

    @DisplayName("USER получает 403 при изменении книги без права WRITE")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCannotUpdateBookWithoutWrite() throws Exception {
        mockMvc.perform(put("/api/books/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookForm(2, List.of(3L, 4L))))
                .andExpect(status().isForbidden());
    }

    @DisplayName("USER получает 403 при создании книги (нужна роль ADMIN)")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCannotCreateBook() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookForm(1, List.of(1L, 2L))))
                .andExpect(status().isForbidden());
    }

    @DisplayName("USER получает 403 при удалении книги (нужна роль ADMIN)")
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userCannotDeleteBook() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- Authenticated ADMIN: full access ----------

    @DisplayName("ADMIN видит все книги")
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminSeesAllBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @DisplayName("ADMIN может прочитать любую книгу")
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadAnyBook() throws Exception {
        mockMvc.perform(get("/api/books/3"))
                .andExpect(status().isOk());
    }

    @DisplayName("ADMIN может создать книгу и для неё создаётся ACL")
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateBook() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookForm(1, List.of(1L, 2L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @DisplayName("ADMIN может удалить книгу")
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDeleteBook() throws Exception {
        mockMvc.perform(delete("/api/books/3"))
                .andExpect(status().isNoContent());
    }
}
