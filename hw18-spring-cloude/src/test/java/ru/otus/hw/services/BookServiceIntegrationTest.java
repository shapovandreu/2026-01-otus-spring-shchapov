package ru.otus.hw.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.fault.FaultInjector;
import ru.otus.hw.models.Book;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Интеграционный тест сервиса книг (проверка ленивой загрузки связей)")
@DataJpaTest
@Import({BookServiceImpl.class, FaultInjector.class})
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @DisplayName("должен загружать книгу со связями без LazyInitializationException при findById")
    @Test
    void shouldLoadBookWithRelationsWhenFindById() {
        var bookOpt = bookService.findById(1L);
        assertThat(bookOpt).isPresent();
        Book book = bookOpt.get();

        assertNotNull(book.getAuthor());
        assertDoesNotThrow(() -> book.getAuthor().getFullName());

        assertNotNull(book.getGenres());
        assertDoesNotThrow(() -> book.getGenres().size());
        assertThat(book.getGenres()).isNotEmpty();
    }

    @DisplayName("должен загружать все книги со связями без LazyInitializationException при findAll")
    @Test
    void shouldLoadAllBooksWithRelationsWhenFindAll() {
        List<Book> books = bookService.findAll();
        assertThat(books).isNotEmpty();

        for (Book book : books) {
            assertNotNull(book.getAuthor());
            assertDoesNotThrow(() -> book.getAuthor().getFullName());

            assertNotNull(book.getGenres());
            assertDoesNotThrow(() -> book.getGenres().size());
            assertThat(book.getGenres()).isNotEmpty();
        }
    }

}