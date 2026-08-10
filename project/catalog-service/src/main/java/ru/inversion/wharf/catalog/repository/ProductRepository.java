package ru.inversion.wharf.catalog.repository;

import java.util.UUID;

import ru.inversion.wharf.catalog.domain.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<Product, UUID> {

    Mono<Product> findByName(String name);
}
