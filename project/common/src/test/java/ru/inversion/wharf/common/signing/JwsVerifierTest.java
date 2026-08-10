package ru.inversion.wharf.common.signing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwsVerifierTest {

    private record Doc(String id, int value) {
    }

    @Test
    void verifiesDocumentSignedByMatchingIssuer() {
        JwsSigner issuer = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(toJson(issuer.jwks()));

        String jws = issuer.sign(new Doc("m-1", 42));

        assertThat(verifier.verify(jws))
                .contains("\"id\":\"m-1\"")
                .contains("\"value\":42");
    }

    @Test
    void rejectsDocumentSignedByAnotherIssuer() {
        JwsSigner issuer = new JwsSigner();
        JwsSigner impostor = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(toJson(issuer.jwks()));

        String forged = impostor.sign(new Doc("m-1", 42));

        assertThatExceptionOfType(JwsVerificationException.class)
                .isThrownBy(() -> verifier.verify(forged));
    }

    @Test
    void rejectsTamperedPayload() {
        JwsSigner issuer = new JwsSigner();
        JwsVerifier verifier = JwsVerifier.fromJwksJson(toJson(issuer.jwks()));

        String jws = issuer.sign(new Doc("m-1", 42));
        String tampered = tamperPayload(jws);

        assertThatExceptionOfType(JwsVerificationException.class)
                .isThrownBy(() -> verifier.verify(tampered));
    }

    private static String toJson(java.util.Map<String, Object> jwks) {
        return com.nimbusds.jose.util.JSONObjectUtils.toJSONString(jwks);
    }

    private static String tamperPayload(String compactJws) {
        String[] parts = compactJws.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'A' ? 'B' : 'A';
        return parts[0] + "." + new String(payload) + "." + parts[2];
    }
}
