package ru.inversion.wharf.telemetry.service;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.telemetry.api.dto.TelemetryRequests.TelemetryEvent;
import ru.inversion.wharf.telemetry.domain.TelemetryEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
class TelemetryPublisherTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID AGENT = UUID.randomUUID();
    private static final UUID INSTALLATION = UUID.randomUUID();
    private static final UUID PRODUCT = UUID.randomUUID();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final TelemetryPublisher publisher =
            new TelemetryPublisher(mock(KafkaSender.class), mapper, "telemetry");

    @Test
    void buildsMessageKeyedByInstallationWithTokenIdentity() {
        TelemetryEvent event = new TelemetryEvent(INSTALLATION, PRODUCT, TelemetryEventType.STATE_CHANGED,
                "running", null, "ok", Instant.parse("2026-07-20T10:00:00Z"));

        SenderRecord<String, String, UUID> record =
                publisher.toSenderRecord(ORG, AGENT, event, Instant.parse("2026-07-20T10:00:01Z"));
        ProducerRecord<String, String> message = record;

        assertThat(message.topic()).isEqualTo("telemetry");
        assertThat(message.key()).isEqualTo(INSTALLATION.toString());
        assertThat(record.correlationMetadata()).isEqualTo(INSTALLATION);
        assertThat(message.value())
                .contains("\"orgId\":\"" + ORG + "\"")
                .contains("\"agentId\":\"" + AGENT + "\"")
                .contains("\"type\":\"state_changed\"")
                .contains("\"occurredAt\":\"2026-07-20T10:00:00Z\"");
    }
}
