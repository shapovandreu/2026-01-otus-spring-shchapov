package ru.inversion.wharf.auth.repository;

import java.util.UUID;

import ru.inversion.wharf.auth.domain.Agent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AgentRepository extends ReactiveCrudRepository<Agent, UUID> {

    Mono<Long> countByOrgId(UUID orgId);
}
