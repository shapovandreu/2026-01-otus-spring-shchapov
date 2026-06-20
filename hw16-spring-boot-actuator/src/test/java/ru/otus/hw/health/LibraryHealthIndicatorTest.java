package ru.otus.hw.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.otus.hw.repositories.BookRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Свой HealthIndicator библиотеки ")
@DataJpaTest
class LibraryHealthIndicatorTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("должен возвращать UP, когда в библиотеке есть книги")
    void shouldReturnUpWhenBooksExist() {
        var indicator = new LibraryHealthIndicator(bookRepository);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("bookCount");
    }

    @Test
    @DisplayName("должен возвращать DOWN, когда библиотека пуста")
    void shouldReturnDownWhenLibraryIsEmpty() {
        bookRepository.deleteAll();
        var indicator = new LibraryHealthIndicator(bookRepository);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("bookCount", 0);
    }
}
