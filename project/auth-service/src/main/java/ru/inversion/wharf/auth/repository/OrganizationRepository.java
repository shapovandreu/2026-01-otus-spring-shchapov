package ru.inversion.wharf.auth.repository;

import java.util.UUID;

import ru.inversion.wharf.auth.domain.Organization;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrganizationRepository extends ReactiveCrudRepository<Organization, UUID> {

    Mono<Organization> findByName(String name);
}
