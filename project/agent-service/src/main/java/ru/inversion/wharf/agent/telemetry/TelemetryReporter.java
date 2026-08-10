package ru.inversion.wharf.agent.telemetry;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.TelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TelemetryReporter {

    private static final Logger log = LoggerFactory.getLogger(TelemetryReporter.class);

    private final ControlPlaneClient controlPlane;

    public TelemetryReporter(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    public Mono<Void> report(AgentTelemetryType type, UUID installationId, UUID productId, String state,
                             UUID releaseId, String message) {
        TelemetryEvent event = new TelemetryEvent(installationId, productId, type.name(), state, releaseId,
                message, Instant.now());
        return controlPlane.sendTelemetry(event)
                .onErrorResume(error -> {
                    log.warn("не удалось отправить телеметрию {}: {}", type, error.toString());
                    return Mono.empty();
                });
    }
}
