package ru.inversion.wharf.catalog.api;

import java.util.UUID;

import ru.inversion.wharf.catalog.api.dto.CatalogResponses;
import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.service.CatalogService;
import ru.inversion.wharf.catalog.service.EntitlementGate;
import ru.inversion.wharf.catalog.service.ManifestService;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/releases")
public class ReleaseController {

    private final CatalogService catalog;
    private final ManifestService manifests;
    private final EntitlementGate entitlements;

    public ReleaseController(CatalogService catalog, ManifestService manifests, EntitlementGate entitlements) {
        this.catalog = catalog;
        this.manifests = manifests;
        this.entitlements = entitlements;
    }

    @GetMapping
    public Flux<CatalogResponses.ReleaseView> releases(@RequestParam("product") UUID productId,
                                                       @RequestParam(required = false) String channel,
                                                       @AuthenticationPrincipal Jwt jwt) {
        Channel requested = channel == null ? Channel.STABLE : Channel.fromSlug(channel);
        UUID orgId = orgOf(jwt);

        return entitlements.require(orgId, productId, requested.slug(), jwt.getTokenValue())
                .thenMany(catalog.publishedReleases(productId, requested))
                .map(CatalogResponses.ReleaseView::of);
    }

    @GetMapping("/{releaseId}/manifest")
    public Mono<CatalogResponses.SignedManifestView> manifest(@PathVariable UUID releaseId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID orgId = orgOf(jwt);
        return catalog.requirePublished(releaseId)
                .flatMap(release -> entitlements
                        .require(orgId, release.productId(), release.channel().slug(), jwt.getTokenValue())
                        .then(manifests.signedManifest(releaseId)))
                .map(CatalogResponses.SignedManifestView::of);
    }

    private static UUID orgOf(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new CatalogExceptions.NotAnAgent();
        }
        return UUID.fromString(org);
    }
}
