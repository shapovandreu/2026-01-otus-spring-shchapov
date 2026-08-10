package ru.inversion.wharf.catalog.repository;

import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Channel;
import ru.inversion.wharf.catalog.domain.Release;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReleaseRepository extends ReactiveCrudRepository<Release, UUID> {

    Flux<Release> findByProductIdAndPublishedIsTrue(UUID productId);

    Flux<Release> findByProductIdAndChannelAndPublishedIsTrue(UUID productId, Channel channel);

    Flux<Release> findByProductId(UUID productId);

    Mono<Long> countByProductId(UUID productId);
}
