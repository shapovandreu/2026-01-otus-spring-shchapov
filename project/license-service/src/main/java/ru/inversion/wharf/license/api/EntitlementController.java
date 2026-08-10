package ru.inversion.wharf.license.api;

import java.util.UUID;

import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.common.audit.AuditPublisher;
import ru.inversion.wharf.license.api.dto.LicenseRequests;
import ru.inversion.wharf.license.api.dto.LicenseResponses;
import ru.inversion.wharf.license.domain.Entitlement;
import ru.inversion.wharf.license.error.LicenseExceptions;
import ru.inversion.wharf.license.service.EntitlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/entitlements")
public class EntitlementController {

    private final EntitlementService entitlements;
    private final AuditPublisher audit;

    public EntitlementController(EntitlementService entitlements, AuditPublisher audit) {
        this.entitlements = entitlements;
        this.audit = audit;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LM', 'ADMIN')")
    public Mono<LicenseResponses.EntitlementView> grant(@Valid @RequestBody LicenseRequests.GrantEntitlement request,
                                                        @AuthenticationPrincipal Jwt actor) {
        return entitlements.grant(request.orgId(), request.productId(), request.channelOrDefault(),
                        request.validUntil())
                .flatMap(entitlement -> recorded(actor, AuditActions.GRANT_ENTITLEMENT, entitlement))
                .map(LicenseResponses.EntitlementView::of);
    }

    @PutMapping("/{entitlementId}")
    @PreAuthorize("hasAnyRole('LM', 'ADMIN')")
    public Mono<LicenseResponses.EntitlementView> update(@PathVariable UUID entitlementId,
                                                         @Valid @RequestBody LicenseRequests.UpdateEntitlement request,
                                                         @AuthenticationPrincipal Jwt actor) {
        return entitlements.changeValidUntil(entitlementId, request.validUntil())
                .flatMap(entitlement -> recorded(actor, AuditActions.GRANT_ENTITLEMENT, entitlement))
                .map(LicenseResponses.EntitlementView::of);
    }

    @DeleteMapping("/{entitlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('LM', 'ADMIN')")
    public Mono<Void> revoke(@PathVariable UUID entitlementId, @AuthenticationPrincipal Jwt actor) {
        return entitlements.revoke(entitlementId)
                .flatMap(entitlement -> recorded(actor, AuditActions.REVOKE_ENTITLEMENT, entitlement))
                .then();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LM', 'ADMIN')")
    public Flux<LicenseResponses.EntitlementView> list(@RequestParam(required = false) UUID orgId) {
        return entitlements.list(orgId).map(LicenseResponses.EntitlementView::of);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('" + Roles.ORG_ADMIN + "', '" + Roles.AGENT + "')")
    public Flux<LicenseResponses.EntitlementView> mine(@AuthenticationPrincipal Jwt jwt) {
        return entitlements.listValid(orgOf(jwt)).map(LicenseResponses.EntitlementView::of);
    }

    @GetMapping("/check")
    public Mono<EntitlementCheck> check(@RequestParam UUID orgId,
                                        @RequestParam UUID productId,
                                        @RequestParam(defaultValue = "stable") String channel) {
        return entitlements.isAllowed(orgId, productId, channel).map(EntitlementCheck::new);
    }

    @GetMapping("/in-use")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Mono<ProductInUse> productInUse(@RequestParam UUID productId) {
        return entitlements.isProductInUse(productId).map(ProductInUse::new);
    }

    private static UUID orgOf(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new LicenseExceptions.MissingOrg();
        }
        return UUID.fromString(org);
    }

    private Mono<Entitlement> recorded(Jwt actor, String action, Entitlement entitlement) {
        return audit.record(actor, action, "entitlement", entitlement.id().toString(), entitlement.orgId())
                .thenReturn(entitlement);
    }

    public record EntitlementCheck(boolean allowed) {
    }

    public record ProductInUse(boolean inUse) {
    }
}
