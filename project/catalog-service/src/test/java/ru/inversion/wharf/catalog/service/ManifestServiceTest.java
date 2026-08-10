package ru.inversion.wharf.catalog.service;

import java.time.Instant;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Caffeine;
import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Product;
import ru.inversion.wharf.catalog.domain.Release;
import ru.inversion.wharf.common.signing.JwsSigner;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManifestServiceTest {

    private static final UUID RELEASE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private CatalogService catalog;

    private final JwsSigner signer = new JwsSigner();

    private ManifestService service() {
        return new ManifestService(catalog, signer, Caffeine.newBuilder().build());
    }

    @Test
    void manifestIsSignedAndVerifiableByPublicKey() throws Exception {
        stubPublishedRelease(Channel.STABLE);

        ManifestService.SignedManifest signed = service().signedManifest(RELEASE_ID).block();

        assertThat(signed).isNotNull();
        assertThat(signed.manifest().version()).isEqualTo("1.4.2");

        JWSObject jws = JWSObject.parse(signed.jws());
        assertThat(jws.verify(new RSASSAVerifier(signer.publicKey()))).isTrue();
        assertThat(jws.getPayload().toJSONObject()).containsEntry("version", "1.4.2");
    }

    @Test
    void manifestIsCachedBetweenPulls() {
        stubPublishedRelease(Channel.STABLE);
        ManifestService service = service();

        StepVerifier.create(service.signedManifest(RELEASE_ID)).expectNextCount(1).verifyComplete();
        StepVerifier.create(service.signedManifest(RELEASE_ID)).expectNextCount(1).verifyComplete();

        verify(catalog, times(1)).requirePublished(RELEASE_ID);
    }

    @Test
    void invalidatingCacheMakesNextPullResign() {
        stubPublishedRelease(Channel.BETA);
        ManifestService service = service();

        ManifestService.SignedManifest beta = service.signedManifest(RELEASE_ID).block();
        assertThat(beta).isNotNull();
        assertThat(beta.manifest().channel()).isEqualTo("beta");

        service.invalidate(RELEASE_ID);
        stubPublishedRelease(Channel.STABLE);

        ManifestService.SignedManifest stable = service.signedManifest(RELEASE_ID).block();
        assertThat(stable).isNotNull();
        assertThat(stable.manifest().channel()).isEqualTo("stable");
    }

    private void stubPublishedRelease(Channel channel) {
        Release release = new Release(RELEASE_ID, PRODUCT_ID, "1.4.2", channel, true, "первый выпуск", Instant.now());
        when(catalog.requirePublished(RELEASE_ID)).thenReturn(Mono.just(release));
        when(catalog.requireProduct(PRODUCT_ID))
                .thenReturn(Mono.just(new Product(PRODUCT_ID, "acme", "gateway", Instant.now())));
    }
}
