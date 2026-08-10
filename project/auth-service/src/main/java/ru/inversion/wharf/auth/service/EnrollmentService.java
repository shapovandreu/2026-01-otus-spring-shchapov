package ru.inversion.wharf.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import ru.inversion.wharf.auth.config.AuthProperties;
import ru.inversion.wharf.auth.domain.Agent;
import ru.inversion.wharf.auth.domain.EnrollmentToken;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.AgentRepository;
import ru.inversion.wharf.auth.repository.EnrollmentTokenRepository;
import ru.inversion.wharf.auth.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EnrollmentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final EnrollmentTokenRepository tokens;
    private final OrganizationRepository organizations;
    private final AgentRepository agents;
    private final JwtIssuer jwtIssuer;
    private final AuthProperties properties;

    public EnrollmentService(EnrollmentTokenRepository tokens,
                             OrganizationRepository organizations,
                             AgentRepository agents,
                             JwtIssuer jwtIssuer,
                             AuthProperties properties) {
        this.tokens = tokens;
        this.organizations = organizations;
        this.agents = agents;
        this.jwtIssuer = jwtIssuer;
        this.properties = properties;
    }

    public Mono<IssuedEnrollmentToken> issue(UUID orgId, Duration ttl) {
        Duration effectiveTtl = ttl != null ? ttl : properties.enrollment().defaultTtl();
        String secret = generateSecret();
        Instant now = Instant.now();

        return organizations.findById(orgId)
                .switchIfEmpty(Mono.error(() -> new AuthExceptions.OrganizationNotFound(orgId)))
                .flatMap(org -> tokens.save(
                        EnrollmentToken.issue(org.id(), sha256(secret), now.plus(effectiveTtl), now)))
                .map(saved -> new IssuedEnrollmentToken(saved.id(), saved.orgId(), secret, saved.expiresAt()));
    }

    public Mono<Void> revoke(UUID tokenId) {
        return tokens.revoke(tokenId).then();
    }

    public Flux<EnrollmentToken> list(UUID orgId) {
        return orgId == null
                ? tokens.findAllByOrderByCreatedAtDesc()
                : tokens.findByOrgIdOrderByCreatedAtDesc(orgId);
    }

    public Mono<EnrolledAgent> enroll(String secret) {
        return tokens.findByTokenHash(sha256(secret))
                .switchIfEmpty(Mono.error(AuthExceptions.TokenInvalid::new))
                .flatMap(this::validate)
                .flatMap(token -> tokens.markUsed(token.id())
                        .filter(updated -> updated > 0)
                        .switchIfEmpty(Mono.error(AuthExceptions.TokenAlreadyUsed::new))
                        .flatMap(burned -> agents.save(Agent.enrolled(token.orgId(), Instant.now()))))
                .flatMap(agent -> organizations.findById(agent.orgId())
                        .switchIfEmpty(Mono.error(() -> new AuthExceptions.OrganizationNotFound(agent.orgId())))
                        .map(org -> new EnrolledAgent(jwtIssuer.forAgent(agent.id(), agent.orgId()),
                                org.id(), org.name())));
    }

    private Mono<EnrollmentToken> validate(EnrollmentToken token) {
        if (token.revoked()) {
            return Mono.error(new AuthExceptions.TokenInvalid());
        }
        if (token.used()) {
            return Mono.error(new AuthExceptions.TokenAlreadyUsed());
        }
        if (token.isExpired(Instant.now())) {
            return Mono.error(new AuthExceptions.TokenExpired());
        }
        return Mono.just(token);
    }

    private static String generateSecret() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен в JVM", e);
        }
    }

    public record IssuedEnrollmentToken(UUID id, UUID orgId, String secret, Instant expiresAt) {
    }

    public record EnrolledAgent(JwtIssuer.IssuedToken token, UUID orgId, String orgName) {
    }
}
