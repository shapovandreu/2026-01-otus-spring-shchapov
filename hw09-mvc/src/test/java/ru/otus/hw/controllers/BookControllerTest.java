package ru.otus.hw.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.services.AuthorService;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;
import ru.otus.hw.services.GenreService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("BookController")
@WebMvcTest(controllers = {BookController.class, GlobalExceptionHandler.class})
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private GenreService genreService;

    @MockitoBean
    private CommentService commentService;

    private Book sampleBook() {
        var author = new Author(1L, "Author_1");
        var genre1 = new Genre(1L, "Genre_1");
        var genre2 = new Genre(2L, "Genre_2");
        return new Book(1L, "BookTitle_1", author, List.of(genre1, genre2));
    }

    @DisplayName("GET /books должен вернуть список книг")
    @Test
    void listShouldReturnBooksView() throws Exception {
        given(bookService.findAll()).willReturn(List.of(sampleBook()));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("books"))
                .andExpect(content().string(containsString("BookTitle_1")))
                .andExpect(content().string(containsString("Author_1")));
    }

    @DisplayName("GET /books/{id} должен показать книгу с комментариями")
    @Test
    void viewShouldReturnBookWithComments() throws Exception {
        var book = sampleBook();
        given(bookService.findById(1L)).willReturn(Optional.of(book));
        given(commentService.findByBookId(1L)).willReturn(List.of(new Comment(10L, "Nice book", book)));

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/view"))
                .andExpect(model().attributeExists("book", "comments"))
                .andExpect(content().string(containsString("BookTitle_1")))
                .andExpect(content().string(containsString("Nice book")));
    }

    @DisplayName("GET /books/{id} должен вернуть 404, если книга не найдена")
    @Test
    void viewShouldReturn404WhenBookMissing() throws Exception {
        given(bookService.findById(42L)).willReturn(Optional.empty());

        mockMvc.perform(get("/books/42"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @DisplayName("GET /books/new должен показать пустую форму с авторами и жанрами")
    @Test
    void newFormShouldRenderEmptyForm() throws Exception {
        given(authorService.findAll()).willReturn(List.of(new Author(1L, "Author_1")));
        given(genreService.findAll()).willReturn(List.of(new Genre(1L, "Genre_1")));

        mockMvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeExists("book", "authors", "genres"));
    }

    @DisplayName("POST /books должен создать книгу и перенаправить на /books")
    @Test
    void createShouldInsertAndRedirect() throws Exception {
        given(bookService.insert(anyString(), anyLong(), any())).willReturn(sampleBook());

        mockMvc.perform(post("/books")
                        .param("title", "NewBook")
                        .param("authorId", "1")
                        .param("genreIds", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        verify(bookService).insert(eq("NewBook"), eq(1L), eq(Set.of(1L, 2L)));
    }

    @DisplayName("POST /books при невалидных данных должен снова показать форму и не вызвать insert")
    @Test
    void createShouldRerenderFormOnValidationErrors() throws Exception {
        given(authorService.findAll()).willReturn(List.of(new Author(1L, "Author_1")));
        given(genreService.findAll()).willReturn(List.of(new Genre(1L, "Genre_1")));

        mockMvc.perform(post("/books")
                        .param("title", "")
                        .param("authorId", "1")
                        .param("genreIds", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeHasFieldErrors("book", "title"));

        verify(bookService, never()).insert(anyString(), anyLong(), any());
    }

    @DisplayName("GET /books/{id}/edit должен заполнить форму существующей книгой")
    @Test
    void editFormShouldPopulateExistingBook() throws Exception {
        given(bookService.findById(1L)).willReturn(Optional.of(sampleBook()));
        given(authorService.findAll()).willReturn(List.of(new Author(1L, "Author_1")));
        given(genreService.findAll()).willReturn(List.of(new Genre(1L, "Genre_1"), new Genre(2L, "Genre_2")));

        mockMvc.perform(get("/books/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeExists("book", "authors", "genres"))
                .andExpect(model().attribute("book",
                        org.hamcrest.Matchers.hasProperty("title", org.hamcrest.Matchers.equalTo("BookTitle_1"))));
    }

    @DisplayName("GET /books/{id}/edit должен вернуть 404, если книга не найдена")
    @Test
    void editFormShouldReturn404WhenBookMissing() throws Exception {
        given(bookService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/books/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @DisplayName("POST /books/{id}/edit должен обновить книгу и перенаправить на просмотр")
    @Test
    void updateShouldSaveAndRedirectToView() throws Exception {
        given(bookService.update(anyLong(), anyString(), anyLong(), any())).willReturn(sampleBook());

        mockMvc.perform(post("/books/1/edit")
                        .param("title", "EditedTitle")
                        .param("authorId", "2")
                        .param("genreIds", "3", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/1"));

        verify(bookService).update(eq(1L), eq("EditedTitle"), eq(2L), eq(Set.of(3L, 4L)));
    }

    @DisplayName("POST /books/{id}/edit при невалидных данных должен снова показать форму без вызова update")
    @Test
    void updateShouldRerenderFormOnValidationErrors() throws Exception {
        given(authorService.findAll()).willReturn(List.of(new Author(1L, "Author_1")));
        given(genreService.findAll()).willReturn(List.of(new Genre(1L, "Genre_1")));

        mockMvc.perform(post("/books/1/edit")
                        .param("title", "")
                        .param("authorId", "1")
                        .param("genreIds", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeHasFieldErrors("book", "title"));

        verify(bookService, never()).update(anyLong(), anyString(), anyLong(), any());
    }

    @DisplayName("POST /books/{id}/delete должен удалить книгу и перенаправить на /books")
    @Test
    void deleteShouldRemoveAndRedirect() throws Exception {
        mockMvc.perform(post("/books/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        verify(bookService, times(1)).deleteById(1L);
    }

    @DisplayName("GET /books/1/delete не должен поддерживаться (удаление не по GET)")
    @Test
    void deleteByGetShouldNotBeAllowed() throws Exception {
        mockMvc.perform(get("/books/1/delete"))
                .andExpect(status().isMethodNotAllowed());

        verify(bookService, never()).deleteById(anyLong());
    }

    @DisplayName("Sanity: возвращаемый список книг конвертируется в DTO без LazyInitException")
    @Test
    void modelShouldContainBookDto() throws Exception {
        var book = sampleBook();
        given(bookService.findAll()).willReturn(List.of(book));

        var mvcResult = mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andReturn();
        var books = mvcResult.getModelAndView().getModel().get("books");
        assertThat(books).isNotNull();
    }
}
