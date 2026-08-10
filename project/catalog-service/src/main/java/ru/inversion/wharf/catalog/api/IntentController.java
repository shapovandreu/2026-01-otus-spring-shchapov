package ru.inversion.wharf.catalog.api;

import java.util.UUID;

import ru.inversion.wharf.catalog.api.dto.CatalogRequests;
import ru.inversion.wharf.catalog.api.dto.CatalogResponses;
import ru.inversion.wharf.catalog.error.CatalogExceptions;
import ru.inversion.wharf.catalog.service.IntentService;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/intents")
public class IntentController {

    private final IntentService intents;

    public IntentController(IntentService intents) {
        this.intents = intents;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('" + Roles.ORG_ADMIN + "', '" + Roles.CLIENT_ENGINEER + "')")
    public Mono<CatalogResponses.IntentView> submit(@Valid @RequestBody CatalogRequests.SubmitIntent request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID orgId = orgOf(jwt);
        return intents.submit(orgId, request.installationId(), request.productId(),
                        request.action(), request.targetReleaseId())
                .map(CatalogResponses.IntentView::of);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.AGENT + "', '" + Roles.ORG_ADMIN + "')")
    public Flux<CatalogResponses.IntentView> queue(@AuthenticationPrincipal Jwt jwt) {
        return intents.pending(orgOf(jwt)).map(CatalogResponses.IntentView::of);
    }

    @PostMapping("/{intentId}/consume")
    @PreAuthorize("hasRole('" + Roles.AGENT + "')")
    public Mono<CatalogResponses.IntentView> consume(@PathVariable UUID intentId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return intents.markConsumed(intentId, orgOf(jwt)).map(CatalogResponses.IntentView::of);
    }

    private static UUID orgOf(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new CatalogExceptions.MissingOrg();
        }
        return UUID.fromString(org);
    }
}
