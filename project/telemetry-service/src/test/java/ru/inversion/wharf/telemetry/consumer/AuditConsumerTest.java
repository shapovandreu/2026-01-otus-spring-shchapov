package ru.inversion.wharf.telemetry.consumer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.common.audit.AuditEvent;
import ru.inversion.wharf.telemetry.domain.AuditDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditConsumerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final AuditConsumer consumer = new AuditConsumer(null, null, mapper);

    @Test
    void parsesAuditJsonIntoMongoDocument() throws Exception {
        UUID org = UUID.randomUUID();
        UUID release = UUID.randomUUID();
        AuditEvent event = new AuditEvent("lmanager", List.of("LM"), AuditActions.PUBLISH_RELEASE,
                "release", release.toString(), org, Instant.parse("2026-07-20T10:00:00Z"));

        AuditDocument document = consumer.toDocument(mapper.writeValueAsString(event));

        assertThat(document.id()).isNull();
        assertThat(document.actor()).isEqualTo("lmanager");
        assertThat(document.roles()).containsExactly("LM");
        assertThat(document.action()).isEqualTo(AuditActions.PUBLISH_RELEASE);
        assertThat(document.targetType()).isEqualTo("release");
        assertThat(document.targetId()).isEqualTo(release.toString());
        assertThat(document.orgId()).isEqualTo(org);
        assertThat(document.occurredAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
    }
}
