package ru.inversion.wharf.auth.api;

import java.util.UUID;

import ru.inversion.wharf.auth.api.dto.OrgAdminRequest;
import ru.inversion.wharf.auth.api.dto.OrgUserResponse;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.service.OrgUserService;
import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.common.audit.AuditPublisher;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/auth/org-admin")
public class OrgAdminController {

    private final OrgUserService orgUsers;
    private final AuditPublisher audit;

    public OrgAdminController(OrgUserService orgUsers, AuditPublisher audit) {
        this.orgUsers = orgUsers;
        this.audit = audit;
    }

    @PostMapping
    @PreAuthorize("hasRole('" + Roles.AGENT + "')")
    public Mono<OrgUserResponse> bootstrap(@Valid @RequestBody OrgAdminRequest request,
                                           @AuthenticationPrincipal Jwt agent) {
        UUID orgId = orgOf(agent);
        return orgUsers.bootstrapAdmin(orgId, request.username(), request.password())
                .flatMap(user -> audit.record(agent, AuditActions.CREATE_ORG_USER, "org-user",
                                user.id().toString(), orgId)
                        .thenReturn(user))
                .map(OrgUserResponse::of);
    }

    private static UUID orgOf(Jwt agent) {
        String org = agent.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new AuthExceptions.MissingOrg();
        }
        return UUID.fromString(org);
    }
}
