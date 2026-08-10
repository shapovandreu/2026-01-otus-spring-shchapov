package ru.inversion.wharf.telemetry.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.inversion.wharf.common.audit.AuditEvent;
import ru.inversion.wharf.telemetry.domain.AuditDocument;
import ru.inversion.wharf.telemetry.repository.AuditRepository;
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
public class AuditConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final KafkaReceiver<String, String> receiver;
    private final AuditRepository repository;
    private final ObjectMapper mapper;

    private volatile Disposable subscription;

    public AuditConsumer(@Qualifier("auditReceiver") KafkaReceiver<String, String> receiver,
                         AuditRepository repository, ObjectMapper mapper) {
        this.receiver = receiver;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void start() {
        subscription = receiver.receive()
                .concatMap(this::handle)
                .subscribe();
        log.info("консюмер аудита запущен");
    }

    private Mono<Void> handle(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> toDocument(record.value()))
                .flatMap(repository::save)
                .doOnError(error -> log.error("не удалось сохранить аудит: {}", error.toString()))
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> record.receiverOffset().acknowledge())
                .then();
    }

    AuditDocument toDocument(String json) throws JsonProcessingException {
        return AuditDocument.from(mapper.readValue(json, AuditEvent.class));
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
