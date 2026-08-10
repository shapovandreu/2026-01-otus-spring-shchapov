package ru.inversion.wharf.agent.fsm;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import ru.inversion.wharf.agent.fsm.IntentCommand.Action;
import ru.inversion.wharf.agent.telemetry.AgentTelemetryType;
import ru.inversion.wharf.agent.telemetry.TelemetryReporter;
import ru.inversion.wharf.agent.verify.ManifestDoc;
import ru.inversion.wharf.agent.verify.TrustAnchors;
import ru.inversion.wharf.agent.verify.VerifyGate;
import ru.inversion.wharf.common.signing.JwsSigner;
import ru.inversion.wharf.common.signing.JwsVerifier;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstallationEngineTest {

    private static final UUID RELEASE = UUID.randomUUID();
    private static final UUID INSTALLATION = UUID.randomUUID();
    private static final UUID PRODUCT = UUID.randomUUID();

    @Mock
    private ControlPlaneClient controlPlane;
    @Mock
    private TrustAnchors trustAnchors;
    @Mock
    private TelemetryReporter telemetry;
    @Mock
    private ArtifactPuller puller;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final VerifyGate gate = new VerifyGate(mapper);
    private final InstallationStore store = new InstallationStore();
    private final JwsSigner catalog = new JwsSigner();

    @BeforeEach
    void trustCatalogAndAcceptTelemetry() {
        JwsVerifier verifier = JwsVerifier.fromJwksJson(JSONObjectUtils.toJSONString(catalog.jwks()));
        when(trustAnchors.catalog()).thenReturn(Mono.just(verifier));
        when(telemetry.report(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    }

    private InstallationEngine engine() {
        return new InstallationEngine(controlPlane, trustAnchors, gate, puller, store, telemetry);
    }

    private ManifestDoc manifest() {
        return new ManifestDoc(RELEASE, PRODUCT, "app", "1.4.2", "stable",
                Instant.parse("2026-07-20T10:00:00Z"));
    }

    private IntentCommand command(Action action) {
        return new IntentCommand(UUID.randomUUID(), action, INSTALLATION, PRODUCT, RELEASE);
    }

    @Test
    void verifiedIntentReachesRunning() {
        when(controlPlane.manifestJws(RELEASE)).thenReturn(Mono.just(catalog.sign(manifest())));

        StepVerifier.create(engine().apply(command(Action.INSTALL)))
                .expectNextMatches(installation -> installation.state() == InstallationState.RUNNING
                        && installation.releaseId().equals(RELEASE)
                        && "1.4.2".equals(installation.version()))
                .verifyComplete();

        assertThat(store.find(INSTALLATION)).get()
                .extracting(Installation::state).isEqualTo(InstallationState.RUNNING);
        verify(puller, times(1)).pull(any());
    }

    @Test
    void manifestSignedByImpostorIsRejected() {
        JwsSigner impostor = new JwsSigner();
        when(controlPlane.manifestJws(RELEASE)).thenReturn(Mono.just(impostor.sign(manifest())));

        StepVerifier.create(engine().apply(command(Action.INSTALL)))
                .expectNextMatches(installation -> installation.state() == InstallationState.REJECTED)
                .verifyComplete();

        verify(telemetry).report(eq(AgentTelemetryType.VERIFY_FAILED), any(), any(), any(), any(), any());
        verify(puller, never()).pull(any());
    }

    @Test
    void reapplyingSameReleaseIsIdempotent() {
        when(controlPlane.manifestJws(RELEASE)).thenReturn(Mono.just(catalog.sign(manifest())));
        InstallationEngine engine = engine();

        StepVerifier.create(engine.apply(command(Action.INSTALL)))
                .expectNextMatches(i -> i.state() == InstallationState.RUNNING)
                .verifyComplete();

        StepVerifier.create(engine.apply(command(Action.UPDATE)))
                .expectNextMatches(i -> i.state() == InstallationState.RUNNING)
                .verifyComplete();

        verify(controlPlane, times(1)).manifestJws(RELEASE);
        verify(puller, times(1)).pull(any());
    }

    @Test
    void uninstallRemovesRunningInstallation() {
        when(controlPlane.manifestJws(RELEASE)).thenReturn(Mono.just(catalog.sign(manifest())));
        InstallationEngine engine = engine();
        engine.apply(command(Action.INSTALL)).block();

        StepVerifier.create(engine.apply(uninstallCommand()))
                .expectNextMatches(installation -> installation.state() == InstallationState.REMOVED
                        && RELEASE.equals(installation.releaseId()))
                .verifyComplete();

        assertThat(store.find(INSTALLATION)).get()
                .extracting(Installation::state).isEqualTo(InstallationState.REMOVED);
        verify(controlPlane, times(1)).manifestJws(RELEASE);
    }

    @Test
    void repeatedUninstallIsIdempotent() {
        when(controlPlane.manifestJws(RELEASE)).thenReturn(Mono.just(catalog.sign(manifest())));
        InstallationEngine engine = engine();
        engine.apply(command(Action.INSTALL)).block();
        engine.apply(uninstallCommand()).block();

        StepVerifier.create(engine.apply(uninstallCommand()))
                .expectNextMatches(installation -> installation.state() == InstallationState.REMOVED)
                .verifyComplete();

        verify(telemetry, times(1)).report(eq(AgentTelemetryType.STATE_CHANGED), any(), any(),
                eq("removed"), any(), any());
    }

    @Test
    void uninstallOfUnknownInstallationIsNoOp() {
        StepVerifier.create(engine().apply(uninstallCommand()))
                .verifyComplete();

        assertThat(store.find(INSTALLATION)).isEmpty();
    }

    private IntentCommand uninstallCommand() {
        return new IntentCommand(UUID.randomUUID(), Action.UNINSTALL, INSTALLATION, PRODUCT, null);
    }
}
