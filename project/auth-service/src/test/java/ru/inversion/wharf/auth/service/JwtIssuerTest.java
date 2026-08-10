package ru.inversion.wharf.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.auth.config.AuthProperties;
import ru.inversion.wharf.auth.security.JwtKeysConfig;
import ru.inversion.wharf.common.api.Roles;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtIssuerTest {

    private final JwtKeysConfig keys = new JwtKeysConfig();
    private final RSAKey rsaKey = keys.rsaKey();
    private final JwtIssuer issuer = new JwtIssuer(keys.jwtEncoder(rsaKey), properties());

    @Test
    void operatorTokenCarriesRolesAndIsVerifiableByPublicKey() throws Exception {
        JwtIssuer.IssuedToken issued = issuer.forOperator("lmanager", List.of("LM"));

        Jwt decoded = decode(issued.token());

        assertThat(decoded.getSubject()).isEqualTo("lmanager");
        assertThat(decoded.getClaimAsStringList(Roles.CLAIM_ROLES)).containsExactly("LM");
        assertThat(decoded.getClaimAsString(Roles.CLAIM_SUBJECT_TYPE)).isEqualTo(Roles.SUBJECT_TYPE_OPERATOR);
        assertThat(decoded.getClaimAsString(Roles.CLAIM_ORG)).isNull();
    }

    @Test
    void agentTokenCarriesOrgAndAgentRole() throws Exception {
        UUID agentId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Jwt decoded = decode(issuer.forAgent(agentId, orgId).token());

        assertThat(decoded.getSubject()).isEqualTo(agentId.toString());
        assertThat(decoded.getClaimAsStringList(Roles.CLAIM_ROLES)).containsExactly(Roles.AGENT);
        assertThat(decoded.getClaimAsString(Roles.CLAIM_ORG)).isEqualTo(orgId.toString());
        assertThat(decoded.getClaimAsString(Roles.CLAIM_SUBJECT_TYPE)).isEqualTo(Roles.SUBJECT_TYPE_AGENT);
    }

    private Jwt decode(String token) throws Exception {
        return NimbusReactiveJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build()
                .decode(token)
                .block();
    }

    private static AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt("https://iw.test/auth", Duration.ofHours(1), Duration.ofHours(24)),
                new AuthProperties.Enrollment(Duration.ofHours(24)),
                List.of());
    }
}
