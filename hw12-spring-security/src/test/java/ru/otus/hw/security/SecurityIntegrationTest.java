package ru.otus.hw.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Security: защита ресурсов")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Страница логина доступна без аутентификации и содержит форму")
    @Test
    void loginPageShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @DisplayName("Защищённые HTML-страницы без аутентификации редиректят на /login")
    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/books",
            "/books/new",
            "/books/1",
            "/books/1/edit",
            "/authors",
            "/genres"
    })
    void protectedHtmlPagesShouldRedirectWhenAnonymous(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @DisplayName("Защищённые REST GET-ресурсы без аутентификации редиректят на /login")
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/books",
            "/api/books/1",
            "/api/books/1/comments",
            "/api/authors",
            "/api/genres"
    })
    void protectedRestGetShouldRedirectWhenAnonymous(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @DisplayName("REST POST без аутентификации редиректит на /login")
    @Test
    void postWithoutAuthShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"authorId\":1,\"genreIds\":[1]}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @DisplayName("REST DELETE без аутентификации редиректит на /login")
    @Test
    void deleteWithoutAuthShouldRedirectToLogin() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @DisplayName("Аутентифицированный пользователь получает HTML-страницы (200)")
    @WithMockUser(username = "user")
    @ParameterizedTest
    @ValueSource(strings = {
            "/books",
            "/books/new",
            "/books/1",
            "/books/1/edit",
            "/authors",
            "/genres"
    })
    void authenticatedUserCanAccessHtmlPages(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @DisplayName("Аутентифицированный пользователь получает REST-ресурсы (200)")
    @WithMockUser(username = "user")
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/books",
            "/api/authors",
            "/api/genres"
    })
    void authenticatedUserCanAccessRestEndpoints(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @DisplayName("Корректные учётные данные через form login успешно аутентифицируют")
    @Test
    void formLoginWithValidCredentialsShouldSucceed() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @DisplayName("Неверный пароль через form login возвращает /login?error")
    @Test
    void formLoginWithInvalidCredentialsShouldFail() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @DisplayName("Несуществующий пользователь через form login возвращает /login?error")
    @Test
    void formLoginWithUnknownUserShouldFail() throws Exception {
        mockMvc.perform(formLogin("/login").user("ghost").password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @DisplayName("Logout редиректит на /login?logout")
    @WithMockUser(username = "user")
    @Test
    void logoutShouldRedirectToLoginPage() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}