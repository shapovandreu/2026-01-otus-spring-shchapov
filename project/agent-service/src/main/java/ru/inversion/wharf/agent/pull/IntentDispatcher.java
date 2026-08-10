package ru.inversion.wharf.agent.pull;

import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.Intent;
import ru.inversion.wharf.agent.fsm.Installation;
import ru.inversion.wharf.agent.fsm.InstallationEngine;
import ru.inversion.wharf.agent.fsm.InstallationState;
import ru.inversion.wharf.agent.fsm.IntentCommand;
import ru.inversion.wharf.agent.telemetry.AgentTelemetryType;
import ru.inversion.wharf.agent.telemetry.TelemetryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class IntentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(IntentDispatcher.class);

    private final InstallationEngine engine;
    private final ControlPlaneClient controlPlane;
    private final TelemetryReporter telemetry;

    public IntentDispatcher(InstallationEngine engine, ControlPlaneClient controlPlane,
                            TelemetryReporter telemetry) {
        this.engine = engine;
        this.controlPlane = controlPlane;
        this.telemetry = telemetry;
    }

    public Mono<Void> dispatch(Intent intent) {
        IntentCommand command = IntentCommand.from(intent);
        return engine.apply(command)
                .flatMap(installation -> confirmIfExecuted(command, installation))
                .then(controlPlane.consumeIntent(intent.id()))
                .doOnSuccess(ignored -> log.debug("намерение {} снято с очереди", intent.id()))
                .onErrorResume(error -> {
                    log.error("намерение {} не исполнено (останется в очереди): {}", intent.id(), error.toString());
                    return Mono.empty();
                });
    }

    private Mono<Void> confirmIfExecuted(IntentCommand command, Installation installation) {
        InstallationState state = installation.state();
        if (state != InstallationState.RUNNING && state != InstallationState.REMOVED) {
            return Mono.empty();
        }
        return telemetry.report(AgentTelemetryType.INTENT_EXECUTED, installation.installationId(),
                installation.productId(), state.name().toLowerCase(), command.releaseId(),
                "intent " + command.id() + " executed").then();
    }
}
