package ru.inversion.wharf.telemetry.service;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.inversion.wharf.telemetry.api.dto.TelemetryRequests.TelemetryEvent;
import ru.inversion.wharf.telemetry.domain.TelemetryRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
public class TelemetryPublisher {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;
    private final String topic;

    public TelemetryPublisher(KafkaSender<String, String> sender, ObjectMapper mapper,
                              @Value("${iw.telemetry.topic}") String topic) {
        this.sender = sender;
        this.mapper = mapper;
        this.topic = topic;
    }

    public Mono<Void> publish(UUID orgId, UUID agentId, TelemetryEvent event) {
        return Mono.fromCallable(() -> toSenderRecord(orgId, agentId, event, Instant.now()))
                .as(sender::send)
                .then();
    }

    SenderRecord<String, String, UUID> toSenderRecord(UUID orgId, UUID agentId, TelemetryEvent event,
                                                      Instant receivedAt) {
        TelemetryRecord record = TelemetryRecord.from(orgId, agentId, event, receivedAt);
        ProducerRecord<String, String> message =
                new ProducerRecord<>(topic, record.installationId().toString(), serialize(record));
        return SenderRecord.create(message, record.installationId());
    }

    private String serialize(TelemetryRecord record) {
        try {
            return mapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать телеметрию", e);
        }
    }
}
