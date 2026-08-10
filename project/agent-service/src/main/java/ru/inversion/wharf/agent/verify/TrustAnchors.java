package ru.inversion.wharf.agent.verify;

import java.util.concurrent.atomic.AtomicReference;

import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import ru.inversion.wharf.common.signing.JwsVerifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TrustAnchors {

    private final ControlPlaneClient controlPlane;
    private final AtomicReference<JwsVerifier> catalog = new AtomicReference<>();

    public TrustAnchors(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    public Mono<JwsVerifier> catalog() {
        return anchor(catalog, controlPlane.catalogJwks());
    }

    private static Mono<JwsVerifier> anchor(AtomicReference<JwsVerifier> cache, Mono<String> jwksSource) {
        JwsVerifier cached = cache.get();
        if (cached != null) {
            return Mono.just(cached);
        }
        return jwksSource.map(JwsVerifier::fromJwksJson).doOnNext(cache::set);
    }
}
