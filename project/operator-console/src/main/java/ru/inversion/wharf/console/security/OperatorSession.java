package ru.inversion.wharf.console.security;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ru.inversion.wharf.common.api.Roles;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record OperatorSession(String token, String username, Set<String> roles, Instant expiresAt) {

    private static final Logger log = LoggerFactory.getLogger(OperatorSession.class);

    public static Optional<OperatorSession> parse(String token) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            List<String> roles = claims.getStringListClaim(Roles.CLAIM_ROLES);
            return Optional.of(new OperatorSession(
                    token,
                    claims.getSubject(),
                    roles == null ? Set.of() : Set.copyOf(roles),
                    claims.getExpirationTime() == null ? null : claims.getExpirationTime().toInstant()));
        } catch (ParseException e) {
            log.debug("Cookie сессии не разбирается как JWT, считаем оператора неаутентифицированным", e);
            return Optional.empty();
        }
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean hasRole(String role) {
        return roles.contains(role) || roles.contains(Roles.ADMIN);
    }

    public boolean canManageCatalog() {
        return hasRole(Roles.RELEASE_MANAGER);
    }

    public boolean canManageLicenses() {
        return hasRole(Roles.LICENSE_MANAGER);
    }

    public boolean canManageOrganizations() {
        return roles.contains(Roles.ADMIN);
    }
}
