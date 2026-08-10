package ru.inversion.wharf.common.audit;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;
    private final String topic;

    public AuditPublisher(KafkaSender<String, String> sender, ObjectMapper mapper, String topic) {
        this.sender = sender;
        this.mapper = mapper;
        this.topic = topic;
    }

    public Mono<Void> record(Jwt actor, String action, String targetType, String targetId, UUID orgId) {
        return record(AuditEvent.of(actor, action, targetType, targetId, orgId));
    }

    public Mono<Void> record(AuditEvent event) {
        return Mono.fromCallable(() -> senderRecord(event))
                .as(sender::send)
                .then()
                .onErrorResume(error -> {
                    log.warn("не удалось записать аудит {}: {}", event.action(), error.toString());
                    return Mono.empty();
                });
    }

    private SenderRecord<String, String, String> senderRecord(AuditEvent event) {
        ProducerRecord<String, String> message = new ProducerRecord<>(topic, event.actor(), serialize(event));
        return SenderRecord.create(message, event.actor());
    }

    private String serialize(AuditEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать аудит", e);
        }
    }
}
