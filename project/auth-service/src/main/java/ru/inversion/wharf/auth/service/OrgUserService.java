package ru.inversion.wharf.auth.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.domain.OperatorUser;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.OperatorUserRepository;
import ru.inversion.wharf.common.api.Roles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrgUserService {

    private final OperatorUserRepository users;
    private final OrganizationService organizations;
    private final PasswordEncoder passwordEncoder;

    public OrgUserService(OperatorUserRepository users, OrganizationService organizations,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<OperatorUser> create(UUID orgId, String username, String password) {
        String login = username.trim();
        return organizations.byId(orgId)
                .then(Mono.defer(() -> users.save(OperatorUser.forOrganization(
                        login, passwordEncoder.encode(password), Roles.ORG_ADMIN, orgId, Instant.now()))))
                .onErrorMap(DataIntegrityViolationException.class, cause -> new AuthExceptions.UserExists(login));
    }

    public Mono<OperatorUser> bootstrapAdmin(UUID orgId, String username, String password) {
        String login = username.trim();
        String hash = passwordEncoder.encode(password);
        return organizations.byId(orgId)
                .then(Mono.defer(() -> users.findByUsername(login)))
                .flatMap(existing -> orgId.equals(existing.orgId())
                        ? users.save(existing.withPasswordHash(hash))
                        : Mono.<OperatorUser>error(new AuthExceptions.UserExists(login)))
                .switchIfEmpty(Mono.defer(() -> users.save(OperatorUser.forOrganization(
                        login, hash, Roles.ORG_ADMIN, orgId, Instant.now()))))
                .onErrorMap(DataIntegrityViolationException.class, cause -> new AuthExceptions.UserExists(login));
    }

    public Flux<OperatorUser> byOrganization(UUID orgId) {
        return users.findByOrgId(orgId);
    }

    public Mono<OperatorUser> delete(UUID userId) {
        return users.findById(userId)
                .filter(OperatorUser::belongsToOrganization)
                .switchIfEmpty(Mono.error(() -> new AuthExceptions.UserNotFound(userId)))
                .flatMap(user -> users.delete(user).thenReturn(user));
    }
}
