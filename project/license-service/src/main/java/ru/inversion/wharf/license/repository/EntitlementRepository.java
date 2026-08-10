package ru.inversion.wharf.license.repository;

import java.util.UUID;

import ru.inversion.wharf.license.domain.Entitlement;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EntitlementRepository extends ReactiveCrudRepository<Entitlement, UUID> {

    Flux<Entitlement> findByOrgId(UUID orgId);

    Mono<Entitlement> findByOrgIdAndProductIdAndChannel(UUID orgId, UUID productId, String channel);

    Mono<Long> countByProductId(UUID productId);
}
