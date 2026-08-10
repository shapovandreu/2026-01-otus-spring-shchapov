package ru.inversion.wharf.agent.pull;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import ru.inversion.wharf.agent.config.AgentProperties;
import ru.inversion.wharf.agent.cp.ControlPlaneClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PullLoop {

    private static final Logger log = LoggerFactory.getLogger(PullLoop.class);

    private final ControlPlaneClient controlPlane;
    private final IntentDispatcher dispatcher;
    private final AgentProperties properties;

    private Disposable subscription;

    public PullLoop(ControlPlaneClient controlPlane, IntentDispatcher dispatcher, AgentProperties properties) {
        this.controlPlane = controlPlane;
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    public void start() {
        Duration interval = properties.pull().interval();
        subscription = Flux.interval(interval)
                .onBackpressureDrop()
                .concatMap(tick -> Mono.delay(nextJitter())
                        .then(runCycle())
                        .onErrorResume(error -> {
                            log.error("сбой pull-цикла (продолжаю опрашивать): {}", error.toString());
                            return Mono.empty();
                        }))
                .subscribe();
        log.info("pull-цикл запущен: интервал {}, джиттер до {}", interval, properties.pull().jitter());
    }

    Mono<Void> runCycle() {
        return controlPlane.intents()
                .concatMap(dispatcher::dispatch)
                .then();
    }

    private Duration nextJitter() {
        long jitterMillis = properties.pull().jitter().toMillis();
        if (jitterMillis <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(jitterMillis + 1));
    }

    @PreDestroy
    void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
