package ru.inversion.wharf.auth.api;

import java.util.UUID;

import ru.inversion.wharf.auth.api.dto.OrgUserRequest;
import ru.inversion.wharf.auth.api.dto.OrgUserResponse;
import ru.inversion.wharf.auth.service.OrgUserService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/auth/orgs/{orgId}/users")
public class OrgUserController {

    private final OrgUserService orgUsers;
    private final AuditPublisher audit;

    public OrgUserController(OrgUserService orgUsers, AuditPublisher audit) {
        this.orgUsers = orgUsers;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LM', 'ADMIN')")
    public Flux<OrgUserResponse> list(@PathVariable UUID orgId) {
        return orgUsers.byOrganization(orgId).map(OrgUserResponse::of);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<OrgUserResponse> create(@PathVariable UUID orgId,
                                        @Valid @RequestBody OrgUserRequest request,
                                        @AuthenticationPrincipal Jwt actor) {
        return orgUsers.create(orgId, request.username(), request.password())
                .flatMap(user -> audit.record(actor, AuditActions.CREATE_ORG_USER, "org-user",
                                user.id().toString(), orgId)
                        .thenReturn(user))
                .map(OrgUserResponse::of);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(@PathVariable UUID orgId, @PathVariable UUID userId,
                             @AuthenticationPrincipal Jwt actor) {
        return orgUsers.delete(userId)
                .flatMap(user -> audit.record(actor, AuditActions.DELETE_ORG_USER, "org-user",
                        user.id().toString(), user.orgId()));
    }
}
