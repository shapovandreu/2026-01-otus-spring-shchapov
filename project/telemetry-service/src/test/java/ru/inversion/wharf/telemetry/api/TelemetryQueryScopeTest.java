package ru.inversion.wharf.telemetry.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.telemetry.query.TelemetryQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelemetryQueryScopeTest {

    private static final UUID OWN_ORG = UUID.randomUUID();
    private static final UUID FOREIGN_ORG = UUID.randomUUID();
    private static final UUID INSTALLATION = UUID.randomUUID();

    @Mock
    private TelemetryQueryService query;

    private TelemetryQueryController controller() {
        return new TelemetryQueryController(query);
    }

    @Test
    void clientCannotWidenScopeByParameter() {
        when(query.installationStatuses(any(), anyLongZero(), anyIntAny())).thenReturn(Flux.empty());

        controller().installations(FOREIGN_ORG, 0, 50, clientJwt()).blockLast();

        verify(query).installationStatuses(eq(OWN_ORG), anyLongZero(), anyIntAny());
    }

    @Test
    void operatorKeepsOrgAsPlainFilter() {
        when(query.installationStatuses(any(), anyLongZero(), anyIntAny())).thenReturn(Flux.empty());

        controller().installations(FOREIGN_ORG, 0, 50, operatorJwt()).blockLast();

        verify(query).installationStatuses(eq(FOREIGN_ORG), anyLongZero(), anyIntAny());
    }

    @Test
    void clientReadsInstallationFeedOnlyWithinOwnOrg() {
        when(query.eventsByInstallationInOrg(any(), any(), any())).thenReturn(Flux.empty());

        controller().events(INSTALLATION, null, 0, 50, clientJwt()).blockLast();

        verify(query).eventsByInstallationInOrg(eq(OWN_ORG), eq(INSTALLATION), any(Pageable.class));
        verify(query, never()).eventsByInstallation(any(), any());
    }

    @Test
    void operatorReadsInstallationFeedDirectly() {
        when(query.eventsByInstallation(any(), any())).thenReturn(Flux.empty());

        controller().events(INSTALLATION, null, 0, 50, operatorJwt()).blockLast();

        verify(query).eventsByInstallation(eq(INSTALLATION), any(Pageable.class));
        verify(query, never()).eventsByInstallationInOrg(any(), any(), any());
    }

    @Test
    void clientWithoutFiltersSeesOwnOrgInsteadOfError() {
        when(query.eventsByOrg(any(), any())).thenReturn(Flux.empty());

        controller().events(null, null, 0, 50, clientJwt()).blockLast();

        verify(query).eventsByOrg(eq(OWN_ORG), any(Pageable.class));
    }

    private static long anyLongZero() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private static int anyIntAny() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static Jwt clientJwt() {
        return jwt(Map.of(
                Roles.CLAIM_ROLES, java.util.List.of(Roles.ORG_ADMIN),
                Roles.CLAIM_ORG, OWN_ORG.toString()));
    }

    private static Jwt operatorJwt() {
        return jwt(Map.of(Roles.CLAIM_ROLES, java.util.List.of(Roles.ADMIN)));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), claims);
    }
}
