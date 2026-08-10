package ru.inversion.wharf.agent.fsm;

import java.time.Instant;

import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import ru.inversion.wharf.agent.telemetry.AgentTelemetryType;
import ru.inversion.wharf.agent.telemetry.TelemetryReporter;
import ru.inversion.wharf.agent.verify.ManifestDoc;
import ru.inversion.wharf.agent.verify.TrustAnchors;
import ru.inversion.wharf.agent.verify.VerifyException;
import ru.inversion.wharf.agent.verify.VerifyGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InstallationEngine {

    private static final Logger log = LoggerFactory.getLogger(InstallationEngine.class);

    private final ControlPlaneClient controlPlane;
    private final TrustAnchors trustAnchors;
    private final VerifyGate verifyGate;
    private final ArtifactPuller puller;
    private final InstallationStore store;
    private final TelemetryReporter telemetry;

    public InstallationEngine(ControlPlaneClient controlPlane, TrustAnchors trustAnchors, VerifyGate verifyGate,
                              ArtifactPuller puller, InstallationStore store, TelemetryReporter telemetry) {
        this.controlPlane = controlPlane;
        this.trustAnchors = trustAnchors;
        this.verifyGate = verifyGate;
        this.puller = puller;
        this.store = store;
        this.telemetry = telemetry;
    }

    public Mono<Installation> apply(IntentCommand command) {
        Installation current = store.find(command.installationId()).orElse(null);
        if (command.isUninstall()) {
            return uninstall(command, current);
        }
        if (current != null && current.isRunningRelease(command.releaseId())) {
            log.debug("инсталляция {} уже RUNNING на релизе {} — идемпотентно пропускаю",
                    command.installationId(), command.releaseId());
            return Mono.just(current);
        }

        log.info("применяю намерение {} на релиз {} (инсталляция {})",
                command.action(), command.releaseId(), command.installationId());

        return trustAnchors.catalog()
                .flatMap(verifier -> controlPlane.manifestJws(command.releaseId())
                        .map(jws -> verifyGate.verifyManifest(verifier, jws)))
                .map(manifest -> deploy(command, manifest))
                .flatMap(this::reportRunning)
                .onErrorResume(VerifyException.class, error -> reject(command, error));
    }

    private Mono<Installation> uninstall(IntentCommand command, Installation current) {
        if (current == null) {
            log.debug("инсталляции {} нет — снимать нечего", command.installationId());
            return Mono.empty();
        }
        if (current.isRemoved()) {
            log.debug("инсталляция {} уже снята — идемпотентно пропускаю", command.installationId());
            return Mono.just(current);
        }

        log.info("снимаю продукт с инсталляции {} (был релиз {})",
                command.installationId(), current.releaseId());
        Installation removed = Installation.removed(current, Instant.now());
        store.save(removed);
        return telemetry.report(AgentTelemetryType.STATE_CHANGED, removed.installationId(), removed.productId(),
                        "removed", removed.releaseId(), "installation removed")
                .thenReturn(removed);
    }

    private Installation deploy(IntentCommand command, ManifestDoc manifest) {
        puller.pull(manifest);
        Installation running = Installation.running(command, manifest, Instant.now());
        store.save(running);
        log.info("инсталляция {} → RUNNING на релизе {} ({})",
                running.installationId(), running.releaseId(), running.version());
        return running;
    }

    private Mono<Installation> reportRunning(Installation running) {
        return telemetry.report(AgentTelemetryType.STATE_CHANGED, running.installationId(), running.productId(),
                        "running", running.releaseId(), "installation running")
                .thenReturn(running);
    }

    private Mono<Installation> reject(IntentCommand command, VerifyException error) {
        log.warn("verify-gate отклонил релиз {}: {}", command.releaseId(), error.getMessage());
        Installation rejected = Installation.rejected(command, Instant.now());
        store.save(rejected);
        return telemetry.report(AgentTelemetryType.VERIFY_FAILED, command.installationId(), command.productId(),
                        "rejected", command.releaseId(), error.getMessage())
                .thenReturn(rejected);
    }
}
