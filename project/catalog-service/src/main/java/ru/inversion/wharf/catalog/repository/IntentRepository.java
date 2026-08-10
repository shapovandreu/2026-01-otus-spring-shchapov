package ru.inversion.wharf.catalog.repository;

import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Intent;
import ru.inversion.wharf.catalog.domain.IntentStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IntentRepository extends ReactiveCrudRepository<Intent, UUID> {

    Flux<Intent> findByOrgIdAndStatusOrderByCreatedAt(UUID orgId, IntentStatus status);

    Mono<Long> countByProductId(UUID productId);
}
