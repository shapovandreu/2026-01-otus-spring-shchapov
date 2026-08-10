package ru.inversion.wharf.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.auth.config.AuthProperties;
import ru.inversion.wharf.common.api.Roles;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtIssuer {

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public JwtIssuer(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken forOperator(String username, List<String> roles) {
        return issue(username, roles, null, Roles.SUBJECT_TYPE_OPERATOR, properties.jwt().operatorTtl());
    }

    public IssuedToken forOrgUser(String username, List<String> roles, UUID orgId) {
        return issue(username, roles, orgId, Roles.SUBJECT_TYPE_CLIENT, properties.jwt().operatorTtl());
    }

    public IssuedToken forAgent(UUID agentId, UUID orgId) {
        return issue(agentId.toString(), List.of(Roles.AGENT), orgId, Roles.SUBJECT_TYPE_AGENT,
                properties.jwt().agentTtl());
    }

    private IssuedToken issue(String subject, List<String> roles, UUID orgId, String subjectType, Duration ttl) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ttl);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(Roles.CLAIM_ROLES, roles)
                .claim(Roles.CLAIM_SUBJECT_TYPE, subjectType);
        if (orgId != null) {
            claims.claim(Roles.CLAIM_ORG, orgId.toString());
        }

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(value, expiresAt, ttl.toSeconds());
    }

    public record IssuedToken(String token, Instant expiresAt, long expiresInSeconds) {
    }
}
