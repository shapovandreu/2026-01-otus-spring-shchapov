package ru.inversion.wharf.auth.repository;

import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OperatorUserRepository extends ReactiveCrudRepository<OperatorUser, UUID> {

    Mono<OperatorUser> findByUsername(String username);

    Flux<OperatorUser> findByOrgId(UUID orgId);
}
