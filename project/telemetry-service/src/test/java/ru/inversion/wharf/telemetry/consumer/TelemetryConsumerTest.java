package ru.inversion.wharf.telemetry.consumer;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.telemetry.domain.TelemetryDocument;
import ru.inversion.wharf.telemetry.domain.TelemetryRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryConsumerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final TelemetryConsumer consumer = new TelemetryConsumer(null, null, mapper);

    @Test
    void parsesKafkaJsonIntoMongoDocument() throws Exception {
        UUID org = UUID.randomUUID();
        UUID agent = UUID.randomUUID();
        UUID installation = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        UUID release = UUID.randomUUID();
        TelemetryRecord record = new TelemetryRecord(org, agent, installation, product, "state_changed",
                "running", release, "installation running",
                Instant.parse("2026-07-20T10:00:00Z"), Instant.parse("2026-07-20T10:00:01Z"));

        TelemetryDocument document = consumer.toDocument(mapper.writeValueAsString(record));

        assertThat(document.id()).isNull();
        assertThat(document.orgId()).isEqualTo(org);
        assertThat(document.agentId()).isEqualTo(agent);
        assertThat(document.installationId()).isEqualTo(installation);
        assertThat(document.type()).isEqualTo("state_changed");
        assertThat(document.state()).isEqualTo("running");
        assertThat(document.releaseId()).isEqualTo(release);
        assertThat(document.occurredAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
        assertThat(document.receivedAt()).isEqualTo(Instant.parse("2026-07-20T10:00:01Z"));
    }
}
