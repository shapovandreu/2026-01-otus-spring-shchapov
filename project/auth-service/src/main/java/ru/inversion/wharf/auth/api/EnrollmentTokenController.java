package ru.inversion.wharf.auth.api;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.api.dto.EnrollmentTokenRequest;
import ru.inversion.wharf.auth.api.dto.EnrollmentTokenResponse;
import ru.inversion.wharf.auth.api.dto.EnrollmentTokenSummary;
import ru.inversion.wharf.auth.service.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/auth/enrollment-tokens")
@PreAuthorize("hasAnyRole('LM', 'ADMIN')")
public class EnrollmentTokenController {

    private final EnrollmentService enrollment;
    private final AuditPublisher audit;

    public EnrollmentTokenController(EnrollmentService enrollment, AuditPublisher audit) {
        this.enrollment = enrollment;
        this.audit = audit;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EnrollmentTokenResponse> issue(@Valid @RequestBody EnrollmentTokenRequest request,
                                               @AuthenticationPrincipal Jwt actor) {
        return enrollment.issue(request.orgId(), request.ttl())
                .flatMap(issued -> audit.record(actor, AuditActions.ISSUE_ENROLLMENT_TOKEN, "enrollment-token",
                                issued.id().toString(), issued.orgId())
                        .thenReturn(issued))
                .map(EnrollmentTokenResponse::of);
    }

    @GetMapping
    public Flux<EnrollmentTokenSummary> list(@RequestParam(required = false) UUID orgId) {
        Instant now = Instant.now();
        return enrollment.list(orgId).map(token -> EnrollmentTokenSummary.of(token, now));
    }

    @DeleteMapping("/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revoke(@PathVariable UUID tokenId, @AuthenticationPrincipal Jwt actor) {
        return enrollment.revoke(tokenId)
                .then(audit.record(actor, AuditActions.REVOKE_ENROLLMENT_TOKEN, "enrollment-token",
                        tokenId.toString(), null));
    }
}
