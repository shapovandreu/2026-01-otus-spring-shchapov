package ru.inversion.wharf.agent.verify;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.common.signing.JwsSigner;
import ru.inversion.wharf.common.signing.JwsVerifier;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class VerifyGateTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final VerifyGate gate = new VerifyGate(mapper);

    private ManifestDoc manifest() {
        return new ManifestDoc(UUID.randomUUID(), UUID.randomUUID(), "app", "1.4.2", "stable",
                Instant.parse("2026-07-20T10:00:00Z"));
    }

    @Test
    void verifiesManifestSignedByCatalog() {
        JwsSigner catalog = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(JSONObjectUtils.toJSONString(catalog.jwks()));
        ManifestDoc original = manifest();

        ManifestDoc verified = gate.verifyManifest(verifier, catalog.sign(original));

        assertThat(verified.releaseId()).isEqualTo(original.releaseId());
        assertThat(verified.version()).isEqualTo("1.4.2");
        assertThat(verified.channel()).isEqualTo("stable");
    }

    @Test
    void rejectsManifestSignedByImpostor() {
        JwsSigner catalog = new JwsSigner();
        JwsSigner impostor = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(JSONObjectUtils.toJSONString(catalog.jwks()));

        String forged = impostor.sign(manifest());

        assertThatExceptionOfType(VerifyException.SignatureInvalid.class)
                .isThrownBy(() -> gate.verifyManifest(verifier, forged));
    }

    @Test
    void rejectsSignedButUnreadablePayload() {
        JwsSigner catalog = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(JSONObjectUtils.toJSONString(catalog.jwks()));

        String signedGarbage = catalog.sign("вовсе не манифест");

        assertThatExceptionOfType(VerifyException.Malformed.class)
                .isThrownBy(() -> gate.verifyManifest(verifier, signedGarbage));
    }
}
