package ru.inversion.wharf.telemetry.api;

import java.util.UUID;

import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.telemetry.api.dto.TelemetryRequests;
import ru.inversion.wharf.telemetry.error.TelemetryExceptions;
import ru.inversion.wharf.telemetry.service.TelemetryPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/telemetry")
public class TelemetryController {

    private final TelemetryPublisher publisher;

    public TelemetryController(TelemetryPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('" + Roles.AGENT + "')")
    public Mono<Void> ingest(@Valid @RequestBody TelemetryRequests.TelemetryEvent event,
                             @AuthenticationPrincipal Jwt jwt) {
        return publisher.publish(orgOf(jwt), agentOf(jwt), event);
    }

    private static UUID orgOf(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        if (org == null || org.isBlank()) {
            throw new TelemetryExceptions.NotAnAgent();
        }
        return UUID.fromString(org);
    }

    private static UUID agentOf(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TelemetryExceptions.NotAnAgent();
        }
    }
}
