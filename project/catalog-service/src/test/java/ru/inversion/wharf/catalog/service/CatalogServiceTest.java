package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.repository.IntentRepository;
import ru.inversion.wharf.catalog.repository.ProductRepository;
import ru.inversion.wharf.catalog.repository.ReleaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final UUID RELEASE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ProductRepository products;
    @Mock
    private ReleaseRepository releases;
    @Mock
    private IntentRepository intents;

    @InjectMocks
    private CatalogService catalog;

    @Test
    void publishingAlreadyPublishedReleaseIsIdempotent() {
        Release published = release(true, Channel.STABLE);
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(published));

        StepVerifier.create(catalog.publish(RELEASE_ID))
                .expectNext(published)
                .verifyComplete();

        verify(releases, never()).save(any());
    }

    @Test
    void publishingDraftMarksItPublished() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(false, Channel.STABLE)));
        when(releases.save(any(Release.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(catalog.publish(RELEASE_ID))
                .expectNextMatches(Release::published)
                .verifyComplete();
    }

    @Test
    void publishedReleaseCanChangeChannel() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(true, Channel.BETA)));
        when(releases.save(any(Release.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(catalog.changeChannel(RELEASE_ID, Channel.STABLE))
                .assertNext(release -> {
                    assertThat(release.channel()).isEqualTo(Channel.STABLE);
                    assertThat(release.version()).isEqualTo("1.4.2");
                    assertThat(release.published()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void changingChannelToTheSameOneIsIdempotent() {
        Release published = release(true, Channel.STABLE);
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(published));

        StepVerifier.create(catalog.changeChannel(RELEASE_ID, Channel.STABLE))
                .expectNext(published)
                .verifyComplete();

        verify(releases, never()).save(any());
    }

    @Test
    void draftReleaseCannotChangeChannel() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(false, Channel.BETA)));

        StepVerifier.create(catalog.changeChannel(RELEASE_ID, Channel.STABLE))
                .expectError(CatalogExceptions.ReleaseNotPublished.class)
                .verify();

        verify(releases, never()).save(any());
    }

    @Test
    void draftReleaseHasNoManifest() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(false, Channel.STABLE)));

        StepVerifier.create(catalog.requirePublished(RELEASE_ID))
                .expectError(CatalogExceptions.ReleaseNotPublished.class)
                .verify();
    }

    @Test
    void unknownReleaseIsNotFound() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(catalog.publish(RELEASE_ID))
                .expectError(CatalogExceptions.ReleaseNotFound.class)
                .verify();
    }

    @Test
    void draftReleaseCanBeEdited() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(false, Channel.BETA)));
        when(releases.save(any(Release.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(catalog.updateDraft(RELEASE_ID, "1.4.3", Channel.STABLE, "исправлено"))
                .assertNext(release -> {
                    assertThat(release.version()).isEqualTo("1.4.3");
                    assertThat(release.channel()).isEqualTo(Channel.STABLE);
                    assertThat(release.changelog()).isEqualTo("исправлено");
                    assertThat(release.published()).isFalse();
                    assertThat(release.productId()).isEqualTo(PRODUCT_ID);
                })
                .verifyComplete();
    }

    @Test
    void publishedReleaseCannotBeEdited() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(true, Channel.STABLE)));

        StepVerifier.create(catalog.updateDraft(RELEASE_ID, "9.9.9", Channel.BETA, "подмена"))
                .expectError(CatalogExceptions.ReleaseAlreadyPublished.class)
                .verify();

        verify(releases, never()).save(any());
    }

    @Test
    void draftReleaseCanBeDeleted() {
        Release draft = release(false, Channel.STABLE);
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(draft));
        when(releases.delete(draft)).thenReturn(Mono.empty());

        StepVerifier.create(catalog.deleteDraft(RELEASE_ID))
                .expectNext(draft)
                .verifyComplete();
    }

    @Test
    void publishedReleaseCannotBeDeleted() {
        when(releases.findById(RELEASE_ID)).thenReturn(Mono.just(release(true, Channel.STABLE)));

        StepVerifier.create(catalog.deleteDraft(RELEASE_ID))
                .expectError(CatalogExceptions.ReleaseAlreadyPublished.class)
                .verify();

        verify(releases, never()).delete(any(Release.class));
    }

    @Test
    void productCanBeRenamed() {
        Product product = product();
        when(products.findById(PRODUCT_ID)).thenReturn(Mono.just(product));
        when(products.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(catalog.updateProduct(PRODUCT_ID, "wharf-next", "новое описание"))
                .assertNext(updated -> {
                    assertThat(updated.name()).isEqualTo("wharf-next");
                    assertThat(updated.description()).isEqualTo("новое описание");
                    assertThat(updated.id()).isEqualTo(PRODUCT_ID);
                })
                .verifyComplete();
    }

    @Test
    void productWithoutReferencesIsDeleted() {
        Product product = product();
        when(products.findById(PRODUCT_ID)).thenReturn(Mono.just(product));
        when(releases.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));
        when(intents.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));
        when(products.delete(product)).thenReturn(Mono.empty());

        StepVerifier.create(catalog.deleteProduct(PRODUCT_ID, Mono.just(false)))
                .expectNext(product)
                .verifyComplete();
    }

    @Test
    void productWithReleasesIsNotDeleted() {
        when(products.findById(PRODUCT_ID)).thenReturn(Mono.just(product()));
        when(releases.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(3L));
        when(intents.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));

        StepVerifier.create(catalog.deleteProduct(PRODUCT_ID, Mono.just(false)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(CatalogExceptions.ProductInUse.class)
                        .hasMessageContaining("релизы (3)"))
                .verify();

        verify(products, never()).delete(any(Product.class));
    }

    @Test
    void productWithEntitlementsIsNotDeleted() {
        when(products.findById(PRODUCT_ID)).thenReturn(Mono.just(product()));
        when(releases.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));
        when(intents.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));

        StepVerifier.create(catalog.deleteProduct(PRODUCT_ID, Mono.just(true)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(CatalogExceptions.ProductInUse.class)
                        .hasMessageContaining("права организаций"))
                .verify();

        verify(products, never()).delete(any(Product.class));
    }

    @Test
    void productWithIntentsIsNotDeleted() {
        when(products.findById(PRODUCT_ID)).thenReturn(Mono.just(product()));
        when(releases.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(0L));
        when(intents.countByProductId(PRODUCT_ID)).thenReturn(Mono.just(2L));

        StepVerifier.create(catalog.deleteProduct(PRODUCT_ID, Mono.just(false)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(CatalogExceptions.ProductInUse.class)
                        .hasMessageContaining("намерения (2)"))
                .verify();

        verify(products, never()).delete(any(Product.class));
    }

    private static Release release(boolean published, Channel channel) {
        return new Release(RELEASE_ID, PRODUCT_ID, "1.4.2", channel, published, "первый выпуск", Instant.now());
    }

    private static Product product() {
        return new Product(PRODUCT_ID, "wharf", "Платформа поставки", Instant.now());
    }
}
