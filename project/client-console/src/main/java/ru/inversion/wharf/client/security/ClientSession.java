package ru.inversion.wharf.client.security;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ru.inversion.wharf.common.api.Roles;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ClientSession(String token, String username, Set<String> roles, UUID orgId, Instant expiresAt) {

    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    public static Optional<ClientSession> parse(String token) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            List<String> roles = claims.getStringListClaim(Roles.CLAIM_ROLES);
            String org = claims.getStringClaim(Roles.CLAIM_ORG);
            return Optional.of(new ClientSession(
                    token,
                    claims.getSubject(),
                    roles == null ? Set.of() : Set.copyOf(roles),
                    org == null || org.isBlank() ? null : UUID.fromString(org),
                    claims.getExpirationTime() == null ? null : claims.getExpirationTime().toInstant()));
        } catch (ParseException | IllegalArgumentException e) {
            log.debug("Cookie сессии не разбирается как JWT, считаем пользователя неаутентифицированным", e);
            return Optional.empty();
        }
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isOrgAdmin() {
        return orgId != null && roles.contains(Roles.ORG_ADMIN);
    }
}
