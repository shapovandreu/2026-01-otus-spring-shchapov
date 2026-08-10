package ru.inversion.wharf.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.telemetry.api.dto.TelemetryRequests.TelemetryEvent;
import ru.inversion.wharf.telemetry.domain.TelemetryEventType;
import ru.inversion.wharf.telemetry.repository.TelemetryRepository;
import ru.inversion.wharf.telemetry.service.TelemetryPublisher;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
class TelemetryPipelineIT {

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    static GenericContainer<?> mongo = new GenericContainer<>("mongo:7")
            .withExposedPorts(27017)
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void kafkaAndMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", () -> "mongodb://%s:%d/iw_telemetry"
                .formatted(mongo.getHost(), mongo.getMappedPort(27017)));
    }

    @Autowired
    private TelemetryPublisher publisher;
    @Autowired
    private TelemetryRepository repository;

    @Test
    void publishedEventTravelsThroughKafkaIntoMongo() {
        UUID org = UUID.randomUUID();
        UUID agent = UUID.randomUUID();
        UUID installation = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        TelemetryEvent event = new TelemetryEvent(installation, product, TelemetryEventType.STATE_CHANGED,
                "running", UUID.randomUUID(), "installation running", Instant.now());

        publisher.publish(org, agent, event).block();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Long stored = repository.findByInstallationIdOrderByReceivedAtDesc(installation, PageRequest.of(0, 10))
                    .count().block();
            assertThat(stored).isPositive();
        });
    }
}
