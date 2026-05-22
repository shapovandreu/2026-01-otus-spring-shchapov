package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.models.Author;
import ru.otus.hw.services.AuthorService;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthorRestController")
@WebMvcTest(controllers = {AuthorRestController.class})
class AuthorRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @DisplayName("GET /api/authors должен вернуть список авторов")
    @Test
    void listShouldReturnJsonArray() throws Exception {
        given(authorService.findAll()).willReturn(List.of(
                new Author(1L, "Author_1"),
                new Author(2L, "Author_2")
        ));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", equalTo(1)))
                .andExpect(jsonPath("$[0].fullName", equalTo("Author_1")))
                .andExpect(jsonPath("$[1].fullName", equalTo("Author_2")));
    }

    @DisplayName("GET /api/authors при пустом списке должен вернуть пустой массив")
    @Test
    void listShouldReturnEmptyArrayWhenNoAuthors() throws Exception {
        given(authorService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
