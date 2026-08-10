package ru.inversion.wharf.catalog.api;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.catalog.api.dto.CatalogResponses;
import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.service.CatalogService;
import ru.inversion.wharf.catalog.service.EntitlementGate;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/catalog/available")
public class ClientCatalogController {

    private final CatalogService catalog;
    private final EntitlementGate entitlements;

    public ClientCatalogController(CatalogService catalog, EntitlementGate entitlements) {
        this.catalog = catalog;
        this.entitlements = entitlements;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.ORG_ADMIN + "', '" + Roles.CLIENT_ENGINEER + "')")
    public Flux<CatalogResponses.AvailableProductView> available(@AuthenticationPrincipal Jwt jwt) {
        requireOrg(jwt);
        return entitlements.mine(jwt.getTokenValue())
                .concatMap(this::describe);
    }

    private Mono<CatalogResponses.AvailableProductView> describe(EntitlementGate.EntitlementView entitlement) {
        Channel channel = Channel.fromSlug(entitlement.channel());
        return catalog.requireProduct(entitlement.productId())
                .flatMap(product -> catalog.publishedReleases(product.id(), channel)
                        .map(CatalogResponses.ReleaseView::of)
                        .collectList()
                        .map(releases -> {
                            List<CatalogResponses.ReleaseView> newestFirst = releases.stream()
                                    .sorted(Comparator.comparing(CatalogResponses.ReleaseView::createdAt).reversed())
                                    .toList();
                            UUID latest = newestFirst.isEmpty() ? null : newestFirst.getFirst().id();
                            return new CatalogResponses.AvailableProductView(product.id(), product.name(),
                                    product.description(), channel.slug(), entitlement.validUntil(),
                                    newestFirst, latest);
                        }))
                .onErrorResume(CatalogExceptions.ProductNotFound.class, missing -> Mono.empty());
    }

    private static void requireOrg(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new CatalogExceptions.MissingOrg();
        }
    }
}
