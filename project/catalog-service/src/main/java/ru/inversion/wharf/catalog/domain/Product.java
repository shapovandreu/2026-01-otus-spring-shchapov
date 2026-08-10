package ru.inversion.wharf.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("product")
public record Product(@Id UUID id, String name, String description, Instant createdAt) {

    public static Product create(String name, String description, Instant now) {
        return new Product(null, name, description, now);
    }

    public Product withDetails(String newName, String newDescription) {
        return new Product(id, newName, newDescription, createdAt);
    }
}
