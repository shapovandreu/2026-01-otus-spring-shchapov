package ru.inversion.wharf.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.auth.config.AuthProperties;
import ru.inversion.wharf.auth.domain.Agent;
import ru.inversion.wharf.auth.domain.EnrollmentToken;
import ru.inversion.wharf.auth.domain.Organization;
import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.AgentRepository;
import ru.inversion.wharf.auth.repository.EnrollmentTokenRepository;
import ru.inversion.wharf.auth.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final String SECRET = "secret-token-value";

    @Mock
    private EnrollmentTokenRepository tokens;
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private AgentRepository agents;
    @Mock
    private JwtIssuer jwtIssuer;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("https://iw.test/auth", Duration.ofHours(1), Duration.ofHours(24)),
                new AuthProperties.Enrollment(Duration.ofHours(24)),
                java.util.List.of());
        service = new EnrollmentService(tokens, organizations, agents, jwtIssuer, properties);
    }

    @Test
    void enrollIssuesAgentTokenWithOrganizationAndBurnsEnrollmentToken() {
        UUID agentId = UUID.randomUUID();
        when(tokens.findByTokenHash(anyString())).thenReturn(Mono.just(validToken()));
        when(tokens.markUsed(TOKEN_ID)).thenReturn(Mono.just(1L));
        when(agents.save(any(Agent.class)))
                .thenReturn(Mono.just(new Agent(agentId, ORG_ID, Agent.STATUS_ACTIVE, Instant.now(), null)));
        when(organizations.findById(ORG_ID))
                .thenReturn(Mono.just(new Organization(ORG_ID, "acme", Instant.now())));
        JwtIssuer.IssuedToken issued = new JwtIssuer.IssuedToken("jwt", Instant.now().plusSeconds(60), 60);
        when(jwtIssuer.forAgent(agentId, ORG_ID)).thenReturn(issued);

        StepVerifier.create(service.enroll(SECRET))
                .expectNext(new EnrollmentService.EnrolledAgent(issued, ORG_ID, "acme"))
                .verifyComplete();

        verify(tokens).markUsed(TOKEN_ID);
    }

    @Test
    void enrollWithUnknownTokenFails() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(service.enroll(SECRET))
                .expectError(AuthExceptions.TokenInvalid.class)
                .verify();

        verify(agents, never()).save(any());
    }

    @Test
    void enrollWithUsedTokenFails() {
        when(tokens.findByTokenHash(anyString()))
                .thenReturn(Mono.just(token(Instant.now().plusSeconds(3600), true, false)));

        StepVerifier.create(service.enroll(SECRET))
                .expectError(AuthExceptions.TokenAlreadyUsed.class)
                .verify();

        verify(agents, never()).save(any());
    }

    @Test
    void enrollWithRevokedTokenFails() {
        when(tokens.findByTokenHash(anyString()))
                .thenReturn(Mono.just(token(Instant.now().plusSeconds(3600), false, true)));

        StepVerifier.create(service.enroll(SECRET))
                .expectError(AuthExceptions.TokenInvalid.class)
                .verify();
    }

    @Test
    void enrollWithExpiredTokenFails() {
        when(tokens.findByTokenHash(anyString()))
                .thenReturn(Mono.just(token(Instant.now().minusSeconds(1), false, false)));

        StepVerifier.create(service.enroll(SECRET))
                .expectError(AuthExceptions.TokenExpired.class)
                .verify();

        verify(agents, never()).save(any());
    }

    @Test
    void enrollLosingRaceForSameTokenFails() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Mono.just(validToken()));
        when(tokens.markUsed(TOKEN_ID)).thenReturn(Mono.just(0L));

        StepVerifier.create(service.enroll(SECRET))
                .expectError(AuthExceptions.TokenAlreadyUsed.class)
                .verify();

        verify(agents, never()).save(any());
    }

    private static EnrollmentToken validToken() {
        return token(Instant.now().plusSeconds(3600), false, false);
    }

    private static EnrollmentToken token(Instant expiresAt, boolean used, boolean revoked) {
        return new EnrollmentToken(TOKEN_ID, ORG_ID, "hash", expiresAt, used, revoked, Instant.now());
    }
}
