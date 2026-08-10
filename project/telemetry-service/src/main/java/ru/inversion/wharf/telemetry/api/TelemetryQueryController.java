package ru.inversion.wharf.telemetry.api;

import java.util.UUID;

import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.telemetry.api.dto.TelemetryResponses.TelemetryView;
import ru.inversion.wharf.telemetry.error.TelemetryExceptions;
import ru.inversion.wharf.telemetry.query.InstallationStatus;
import ru.inversion.wharf.telemetry.query.TelemetryQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/telemetry")
public class TelemetryQueryController {

    private final TelemetryQueryService query;

    public TelemetryQueryController(TelemetryQueryService query) {
        this.query = query;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN', '" + Roles.ORG_ADMIN + "')")
    public Flux<TelemetryView> events(@RequestParam(required = false) UUID installation,
                                      @RequestParam(required = false) UUID org,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "100") int limit,
                                      @AuthenticationPrincipal Jwt jwt) {
        PageRequest pageRequest = QueryPage.of(page, limit);
        UUID ownOrg = clientOrgOf(jwt);

        if (ownOrg != null) {
            return (installation == null
                    ? query.eventsByOrg(ownOrg, pageRequest)
                    : query.eventsByInstallationInOrg(ownOrg, installation, pageRequest))
                    .map(TelemetryView::of);
        }
        if (installation != null) {
            return query.eventsByInstallation(installation, pageRequest).map(TelemetryView::of);
        }
        if (org != null) {
            return query.eventsByOrg(org, pageRequest).map(TelemetryView::of);
        }
        return Flux.error(new TelemetryExceptions.QueryFilterRequired());
    }

    @GetMapping("/installations")
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN', '" + Roles.ORG_ADMIN + "')")
    public Flux<InstallationStatus> installations(@RequestParam(required = false) UUID org,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int limit,
                                                  @AuthenticationPrincipal Jwt jwt) {
        UUID ownOrg = clientOrgOf(jwt);
        UUID scope = ownOrg == null ? org : ownOrg;
        return query.installationStatuses(scope, QueryPage.offset(page, limit), QueryPage.size(limit));
    }

    private static UUID clientOrgOf(Jwt jwt) {
        String org = jwt.getClaimAsString(Roles.CLAIM_ORG);
        return org == null || org.isBlank() ? null : UUID.fromString(org);
    }
}
