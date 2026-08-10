package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Intent;
import ru.inversion.wharf.catalog.domain.IntentAction;
import ru.inversion.wharf.catalog.domain.IntentStatus;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.repository.IntentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentServiceTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID INSTALLATION = UUID.randomUUID();
    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID OTHER_PRODUCT = UUID.randomUUID();
    private static final UUID RELEASE = UUID.randomUUID();

    @Mock
    private IntentRepository intents;
    @Mock
    private CatalogService catalog;

    @InjectMocks
    private IntentService service;

    @Test
    void submitQueuesPendingIntentForPublishedRelease() {
        when(catalog.requireProduct(PRODUCT)).thenReturn(Mono.just(Product.create("acme", "app", Instant.now())));
        when(catalog.requirePublished(RELEASE)).thenReturn(Mono.just(release(PRODUCT)));
        when(intents.save(any(Intent.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(service.submit(ORG, INSTALLATION, PRODUCT, IntentAction.INSTALL, RELEASE))
                .expectNextMatches(intent -> intent.status() == IntentStatus.PENDING
                        && intent.orgId().equals(ORG)
                        && intent.targetReleaseId().equals(RELEASE))
                .verifyComplete();
    }

    @Test
    void submitRejectsReleaseFromAnotherProduct() {
        when(catalog.requireProduct(PRODUCT)).thenReturn(Mono.just(Product.create("acme", "app", Instant.now())));
        when(catalog.requirePublished(RELEASE)).thenReturn(Mono.just(release(OTHER_PRODUCT)));

        StepVerifier.create(service.submit(ORG, INSTALLATION, PRODUCT, IntentAction.INSTALL, RELEASE))
                .expectError(CatalogExceptions.ReleaseProductMismatch.class)
                .verify();

        verify(intents, never()).save(any());
    }

    @Test
    void submitRejectsUnpublishedTarget() {
        when(catalog.requireProduct(PRODUCT)).thenReturn(Mono.just(Product.create("acme", "app", Instant.now())));
        when(catalog.requirePublished(RELEASE))
                .thenReturn(Mono.error(new CatalogExceptions.ReleaseNotPublished(RELEASE)));

        StepVerifier.create(service.submit(ORG, INSTALLATION, PRODUCT, IntentAction.INSTALL, RELEASE))
                .expectError(CatalogExceptions.ReleaseNotPublished.class)
                .verify();

        verify(intents, never()).save(any());
    }

    @Test
    void markConsumedRejectsIntentOfAnotherOrg() {
        Intent foreign = Intent.queued(UUID.randomUUID(), INSTALLATION, PRODUCT, IntentAction.INSTALL, RELEASE,
                Instant.now());
        UUID intentId = UUID.randomUUID();
        when(intents.findById(intentId)).thenReturn(Mono.just(foreign));

        StepVerifier.create(service.markConsumed(intentId, ORG))
                .expectError(CatalogExceptions.IntentNotFound.class)
                .verify();

        verify(intents, never()).save(any());
    }

    private static Release release(UUID productId) {
        return new Release(RELEASE, productId, "1.4.2", Channel.STABLE, true, null, Instant.now());
    }
}
