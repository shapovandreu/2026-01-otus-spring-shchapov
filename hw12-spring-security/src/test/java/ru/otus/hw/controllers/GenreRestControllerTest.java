package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.models.Genre;
import ru.otus.hw.repositories.UserRepository;
import ru.otus.hw.security.SecurityConfig;
import ru.otus.hw.services.CustomUserDetailsService;
import ru.otus.hw.services.GenreService;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GenreRestController")
@WebMvcTest(controllers = {GenreRestController.class})
@Import({SecurityConfig.class, CustomUserDetailsService.class})
@WithMockUser
class GenreRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenreService genreService;

    @MockitoBean
    private UserRepository userRepository;

    @DisplayName("GET /api/genres должен вернуть список жанров")
    @Test
    void listShouldReturnJsonArray() throws Exception {
        given(genreService.findAll()).willReturn(List.of(
                new Genre(1L, "Genre_1"),
                new Genre(2L, "Genre_2"),
                new Genre(3L, "Genre_3")
        ));

        mockMvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", equalTo(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Genre_1")))
                .andExpect(jsonPath("$[2].name", equalTo("Genre_3")));
    }

    @DisplayName("GET /api/genres при пустом списке должен вернуть пустой массив")
    @Test
    void listShouldReturnEmptyArrayWhenNoGenres() throws Exception {
        given(genreService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
