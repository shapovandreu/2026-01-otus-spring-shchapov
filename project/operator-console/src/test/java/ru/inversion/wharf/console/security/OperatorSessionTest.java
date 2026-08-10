package ru.inversion.wharf.console.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import ru.inversion.wharf.common.api.Roles;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorSessionTest {

    private static RSAKey key;

    @BeforeAll
    static void generateKey() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test").generate();
    }

    @Test
    void readsUsernameAndRoles() throws Exception {
        String token = jwt("rmanager", List.of(Roles.RELEASE_MANAGER), Instant.now().plusSeconds(3600));

        OperatorSession session = OperatorSession.parse(token).orElseThrow();

        assertThat(session.username()).isEqualTo("rmanager");
        assertThat(session.roles()).containsExactly(Roles.RELEASE_MANAGER);
        assertThat(session.canManageCatalog()).isTrue();
        assertThat(session.canManageLicenses()).isFalse();
        assertThat(session.canManageOrganizations()).isFalse();
    }

    @Test
    void adminReachesEveryScreen() throws Exception {
        String token = jwt("admin", List.of(Roles.ADMIN), Instant.now().plusSeconds(3600));

        OperatorSession session = OperatorSession.parse(token).orElseThrow();

        assertThat(session.canManageCatalog()).isTrue();
        assertThat(session.canManageLicenses()).isTrue();
        assertThat(session.canManageOrganizations()).isTrue();
    }

    @Test
    void licenseManagerDoesNotOnboardOrganizations() throws Exception {
        String token = jwt("lmanager", List.of(Roles.LICENSE_MANAGER), Instant.now().plusSeconds(3600));

        OperatorSession session = OperatorSession.parse(token).orElseThrow();

        assertThat(session.canManageLicenses()).isTrue();
        assertThat(session.canManageOrganizations()).isFalse();
    }

    @Test
    void detectsExpiredToken() throws Exception {
        String token = jwt("admin", List.of(Roles.ADMIN), Instant.now().minusSeconds(60));

        OperatorSession session = OperatorSession.parse(token).orElseThrow();

        assertThat(session.isExpired(Instant.now())).isTrue();
    }

    @Test
    void unreadableCookieIsNotASession() {
        assertThat(OperatorSession.parse("не-jwt-вовсе")).isEmpty();
    }

    private static String jwt(String subject, List<String> roles, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim(Roles.CLAIM_ROLES, roles)
                .claim(Roles.CLAIM_SUBJECT_TYPE, Roles.SUBJECT_TYPE_OPERATOR)
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }

    @Test
    void missingRolesClaimYieldsNoRoles() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("stranger").build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key.toPrivateKey()));

        Optional<OperatorSession> session = OperatorSession.parse(jwt.serialize());

        assertThat(session).isPresent();
        assertThat(session.orElseThrow().roles()).isEmpty();
        assertThat(session.orElseThrow().canManageCatalog()).isFalse();
    }
}
