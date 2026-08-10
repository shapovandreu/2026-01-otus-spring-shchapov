package ru.inversion.wharf.telemetry.repository;

import java.util.UUID;

import ru.inversion.wharf.telemetry.domain.AuditDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AuditRepository extends ReactiveMongoRepository<AuditDocument, String> {

    Flux<AuditDocument> findByActorOrderByOccurredAtDesc(String actor, Pageable pageable);

    Flux<AuditDocument> findByOrgIdOrderByOccurredAtDesc(UUID orgId, Pageable pageable);

    Flux<AuditDocument> findByActionOrderByOccurredAtDesc(String action, Pageable pageable);

    Flux<AuditDocument> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
