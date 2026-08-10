package ru.inversion.wharf.auth.repository;

import java.util.UUID;

import ru.inversion.wharf.auth.domain.EnrollmentToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EnrollmentTokenRepository extends ReactiveCrudRepository<EnrollmentToken, UUID> {

    Mono<EnrollmentToken> findByTokenHash(String tokenHash);

    Flux<EnrollmentToken> findAllByOrderByCreatedAtDesc();

    Flux<EnrollmentToken> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    Mono<Long> countByOrgId(UUID orgId);

    @Modifying
    @Query("UPDATE enrollment_token SET used = true WHERE id = :id AND used = false AND revoked = false")
    Mono<Long> markUsed(UUID id);

    @Modifying
    @Query("UPDATE enrollment_token SET revoked = true WHERE id = :id AND used = false")
    Mono<Long> revoke(UUID id);
}
