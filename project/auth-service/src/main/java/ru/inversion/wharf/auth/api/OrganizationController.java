package ru.inversion.wharf.auth.api;

import java.util.UUID;

import ru.inversion.wharf.auth.api.dto.OrganizationRequest;
import ru.inversion.wharf.auth.api.dto.OrganizationResponse;
import ru.inversion.wharf.auth.domain.Organization;
import ru.inversion.wharf.auth.service.OrganizationService;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.common.audit.AuditPublisher;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/auth/orgs")
public class OrganizationController {

    private final OrganizationService organizations;
    private final AuditPublisher audit;

    public OrganizationController(OrganizationService organizations, AuditPublisher audit) {
        this.organizations = organizations;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Flux<OrganizationResponse> list() {
        return organizations.list().map(OrganizationResponse::of);
    }

    @GetMapping("/{orgId}")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Mono<OrganizationResponse> byId(@PathVariable UUID orgId) {
        return organizations.byId(orgId).map(OrganizationResponse::of);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request,
                                             @AuthenticationPrincipal Jwt actor) {
        return organizations.create(request.name())
                .flatMap(organization -> recorded(actor, AuditActions.CREATE_ORGANIZATION, organization))
                .map(OrganizationResponse::of);
    }

    @PutMapping("/{orgId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<OrganizationResponse> rename(@PathVariable UUID orgId,
                                             @Valid @RequestBody OrganizationRequest request,
                                             @AuthenticationPrincipal Jwt actor) {
        return organizations.rename(orgId, request.name())
                .flatMap(organization -> recorded(actor, AuditActions.RENAME_ORGANIZATION, organization))
                .map(OrganizationResponse::of);
    }

    @DeleteMapping("/{orgId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(@PathVariable UUID orgId, @AuthenticationPrincipal Jwt actor) {
        return organizations.delete(orgId)
                .flatMap(organization -> recorded(actor, AuditActions.DELETE_ORGANIZATION, organization))
                .then();
    }

    private Mono<Organization> recorded(Jwt actor, String action, Organization organization) {
        return audit.record(actor, action, "organization", organization.id().toString(), organization.id())
                .thenReturn(organization);
    }
}
