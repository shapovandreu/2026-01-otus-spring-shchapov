package ru.inversion.wharf.catalog;

import java.time.Duration;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.service.CatalogService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
class CatalogPersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private CatalogService catalog;

    @Test
    void releaseSurvivesRoundTripThroughRealDatabase() {
        Product product = catalog.createProduct("acme", "app").block();
        Release release = catalog.createRelease(product.id(), "1.0.0", Channel.BETA,
                "первый выпуск: базовый функционал").block();

        assertThat(release).isNotNull();
        assertThat(release.published()).isFalse();

        catalog.publish(release.id()).block();

        StepVerifier.create(catalog.publishedReleases(product.id(), Channel.BETA))
                .assertNext(published -> {
                    assertThat(published.version()).isEqualTo("1.0.0");
                    assertThat(published.changelog()).isEqualTo("первый выпуск: базовый функционал");
                })
                .verifyComplete();
    }

    @Test
    void publishedReleaseMovesBetweenChannelsOnRealDatabase() {
        Product product = catalog.createProduct("acme-2", "app").block();
        Release release = catalog.createRelease(product.id(), "2.0.0", Channel.BETA, null).block();
        catalog.publish(release.id()).block();

        catalog.changeChannel(release.id(), Channel.STABLE).block();

        StepVerifier.create(catalog.publishedReleases(product.id(), Channel.BETA))
                .verifyComplete();
        StepVerifier.create(catalog.publishedReleases(product.id(), Channel.STABLE))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void draftReleaseRejectsChannelChangeOnRealDatabase() {
        Product product = catalog.createProduct("acme-3", "app").block();
        Release draft = catalog.createRelease(product.id(), "3.0.0", Channel.BETA, null).block();

        StepVerifier.create(catalog.changeChannel(draft.id(), Channel.STABLE))
                .expectError(CatalogExceptions.ReleaseNotPublished.class)
                .verify();
    }
}
