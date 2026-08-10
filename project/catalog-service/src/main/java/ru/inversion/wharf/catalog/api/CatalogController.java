package ru.inversion.wharf.catalog.api;

import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.catalog.api.dto.CatalogRequests;
import ru.inversion.wharf.catalog.api.dto.CatalogResponses;
import ru.inversion.wharf.catalog.service.CatalogService;
import ru.inversion.wharf.catalog.service.EntitlementGate;
import ru.inversion.wharf.catalog.service.ManifestService;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.common.audit.AuditPublisher;
import ru.inversion.wharf.common.signing.JwsSigner;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/catalog")
public class CatalogController {

    private final CatalogService catalog;
    private final ManifestService manifests;
    private final JwsSigner signer;
    private final AuditPublisher audit;
    private final EntitlementGate entitlements;

    public CatalogController(CatalogService catalog, ManifestService manifests, JwsSigner signer,
                             AuditPublisher audit, EntitlementGate entitlements) {
        this.catalog = catalog;
        this.manifests = manifests;
        this.signer = signer;
        this.audit = audit;
        this.entitlements = entitlements;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ProductView> createProduct(@Valid @RequestBody CatalogRequests.CreateProduct request) {
        return catalog.createProduct(request.name(), request.description())
                .map(CatalogResponses.ProductView::of);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Flux<CatalogResponses.ProductView> products() {
        return catalog.allProducts().map(CatalogResponses.ProductView::of);
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ProductView> updateProduct(@PathVariable UUID productId,
                                                            @Valid @RequestBody CatalogRequests.UpdateProduct request,
                                                            @AuthenticationPrincipal Jwt actor) {
        return catalog.updateProduct(productId, request.name(), request.description())
                .flatMap(product -> audit.record(actor, AuditActions.UPDATE_PRODUCT, "product",
                                product.id().toString(), null)
                        .thenReturn(product))
                .map(CatalogResponses.ProductView::of);
    }

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<Void> deleteProduct(@PathVariable UUID productId, @AuthenticationPrincipal Jwt actor) {
        return catalog.deleteProduct(productId, entitlements.isProductInUse(productId, actor.getTokenValue()))
                .flatMap(product -> audit.record(actor, AuditActions.DELETE_PRODUCT, "product",
                        product.id().toString(), null));
    }

    @PostMapping("/products/{productId}/releases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ReleaseView> createRelease(@PathVariable UUID productId,
                                                            @Valid @RequestBody CatalogRequests.CreateRelease request) {
        return catalog.createRelease(productId, request.version(), request.channelOrDefault(), request.changelog())
                .map(CatalogResponses.ReleaseView::of);
    }

    @GetMapping("/products/{productId}/releases")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Flux<CatalogResponses.ReleaseView> releases(@PathVariable UUID productId) {
        return catalog.releasesForOperator(productId).map(CatalogResponses.ReleaseView::of);
    }

    @PostMapping("/releases/{releaseId}/publish")
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ReleaseView> publish(@PathVariable UUID releaseId,
                                                      @AuthenticationPrincipal Jwt actor) {
        return catalog.publish(releaseId)
                .flatMap(release -> audit.record(actor, AuditActions.PUBLISH_RELEASE, "release",
                                release.id().toString(), null)
                        .thenReturn(release))
                .map(CatalogResponses.ReleaseView::of);
    }

    @PatchMapping("/releases/{releaseId}/channel")
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ReleaseView> changeChannel(@PathVariable UUID releaseId,
                                                            @Valid @RequestBody CatalogRequests.ChangeChannel request,
                                                            @AuthenticationPrincipal Jwt actor) {
        return catalog.changeChannel(releaseId, request.channel())
                .doOnNext(release -> manifests.invalidate(release.id()))
                .flatMap(release -> audit.record(actor, AuditActions.CHANGE_RELEASE_CHANNEL, "release",
                                release.id().toString(), null)
                        .thenReturn(release))
                .map(CatalogResponses.ReleaseView::of);
    }

    @PatchMapping("/releases/{releaseId}")
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<CatalogResponses.ReleaseView> updateRelease(@PathVariable UUID releaseId,
                                                            @Valid @RequestBody CatalogRequests.UpdateRelease request,
                                                            @AuthenticationPrincipal Jwt actor) {
        return catalog.updateDraft(releaseId, request.version(), request.channelOrDefault(), request.changelog())
                .flatMap(release -> audit.record(actor, AuditActions.UPDATE_RELEASE, "release",
                                release.id().toString(), null)
                        .thenReturn(release))
                .map(CatalogResponses.ReleaseView::of);
    }

    @DeleteMapping("/releases/{releaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RM', 'ADMIN')")
    public Mono<Void> deleteRelease(@PathVariable UUID releaseId, @AuthenticationPrincipal Jwt actor) {
        return catalog.deleteDraft(releaseId)
                .flatMap(release -> audit.record(actor, AuditActions.DELETE_RELEASE, "release",
                        release.id().toString(), null));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return signer.jwks();
    }
}
