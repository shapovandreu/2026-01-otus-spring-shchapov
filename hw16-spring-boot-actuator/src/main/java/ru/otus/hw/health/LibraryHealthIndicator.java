package ru.otus.hw.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import ru.otus.hw.repositories.BookRepository;

@Component
@RequiredArgsConstructor
public class LibraryHealthIndicator implements HealthIndicator {

    private final BookRepository bookRepository;

    @Override
    public Health health() {
        try {
            long bookCount = bookRepository.count();
            if (bookCount == 0) {
                return Health.down()
                        .withDetail("message", "Library is empty: no books available")
                        .withDetail("bookCount", 0)
                        .build();
            }
            return Health.up()
                    .withDetail("message", "Library is operational")
                    .withDetail("bookCount", bookCount)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("message", "Library data store is not reachable")
                    .build();
        }
    }
}
