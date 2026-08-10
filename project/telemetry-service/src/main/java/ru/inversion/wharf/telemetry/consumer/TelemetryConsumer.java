package ru.inversion.wharf.telemetry.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.inversion.wharf.telemetry.domain.TelemetryDocument;
import ru.inversion.wharf.telemetry.domain.TelemetryRecord;
import ru.inversion.wharf.telemetry.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Component
public class TelemetryConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final KafkaReceiver<String, String> receiver;
    private final TelemetryRepository repository;
    private final ObjectMapper mapper;

    private volatile Disposable subscription;

    public TelemetryConsumer(@Qualifier("telemetryReceiver") KafkaReceiver<String, String> receiver,
                             TelemetryRepository repository, ObjectMapper mapper) {
        this.receiver = receiver;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void start() {
        subscription = receiver.receive()
                .concatMap(this::handle)
                .subscribe();
        log.info("консюмер телеметрии запущен");
    }

    private Mono<Void> handle(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> toDocument(record.value()))
                .flatMap(repository::save)
                .doOnError(error -> log.error("не удалось сохранить телеметрию: {}", error.toString()))
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> record.receiverOffset().acknowledge())
                .then();
    }

    TelemetryDocument toDocument(String json) throws JsonProcessingException {
        TelemetryRecord record = mapper.readValue(json, TelemetryRecord.class);
        return TelemetryDocument.from(record);
    }

    @Override
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null && !subscription.isDisposed();
    }
}
