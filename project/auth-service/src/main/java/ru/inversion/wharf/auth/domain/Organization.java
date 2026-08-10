package ru.inversion.wharf.auth.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("organization")
public record Organization(@Id UUID id, String name, Instant createdAt) {

    public Organization renamedTo(String newName) {
        return new Organization(id, newName, createdAt);
    }
}
