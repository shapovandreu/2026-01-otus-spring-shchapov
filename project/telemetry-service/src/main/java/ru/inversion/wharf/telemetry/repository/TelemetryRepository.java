package ru.inversion.wharf.telemetry.repository;

import java.util.UUID;

import ru.inversion.wharf.telemetry.domain.TelemetryDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TelemetryRepository extends ReactiveMongoRepository<TelemetryDocument, String> {

    Flux<TelemetryDocument> findByInstallationIdOrderByReceivedAtDesc(UUID installationId, Pageable pageable);

    Flux<TelemetryDocument> findByOrgIdOrderByReceivedAtDesc(UUID orgId, Pageable pageable);

    Flux<TelemetryDocument> findByOrgIdAndInstallationIdOrderByReceivedAtDesc(
            UUID orgId, UUID installationId, Pageable pageable);
}
