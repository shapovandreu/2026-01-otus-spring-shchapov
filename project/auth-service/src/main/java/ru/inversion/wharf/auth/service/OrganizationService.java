package ru.inversion.wharf.auth.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.Organization;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.AgentRepository;
import ru.inversion.wharf.auth.repository.EnrollmentTokenRepository;
import ru.inversion.wharf.auth.repository.OrganizationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final AgentRepository agents;
    private final EnrollmentTokenRepository tokens;

    public OrganizationService(OrganizationRepository organizations, AgentRepository agents,
                               EnrollmentTokenRepository tokens) {
        this.organizations = organizations;
        this.agents = agents;
        this.tokens = tokens;
    }

    public Flux<Organization> list() {
        return organizations.findAll();
    }

    public Mono<Organization> byId(UUID id) {
        return organizations.findById(id)
                .switchIfEmpty(Mono.error(() -> new AuthExceptions.OrganizationNotFound(id)));
    }

    public Mono<Organization> create(String name) {
        String trimmed = name.trim();
        return organizations.save(new Organization(null, trimmed, Instant.now()))
                .onErrorMap(DataIntegrityViolationException.class,
                        cause -> new AuthExceptions.OrganizationExists(trimmed));
    }

    public Mono<Organization> rename(UUID id, String name) {
        String trimmed = name.trim();
        return byId(id)
                .flatMap(organization -> organizations.save(organization.renamedTo(trimmed)))
                .onErrorMap(DataIntegrityViolationException.class,
                        cause -> new AuthExceptions.OrganizationExists(trimmed));
    }

    public Mono<Organization> delete(UUID id) {
        return byId(id)
                .flatMap(organization -> Mono.zip(agents.countByOrgId(id), tokens.countByOrgId(id))
                        .flatMap(counts -> counts.getT1() + counts.getT2() > 0
                                ? Mono.error(new AuthExceptions.OrganizationInUse(id))
                                : organizations.delete(organization).thenReturn(organization)));
    }
}
