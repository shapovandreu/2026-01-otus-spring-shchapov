package ru.inversion.wharf.agent.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.inversion.wharf.common.signing.JwsVerifier;
import ru.inversion.wharf.common.signing.JwsVerificationException;
import org.springframework.stereotype.Component;

@Component
public class VerifyGate {

    private final ObjectMapper mapper;

    public VerifyGate(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ManifestDoc verifyManifest(JwsVerifier verifier, String jws) {
        return parse(verifiedPayload(verifier, jws), ManifestDoc.class);
    }

    private static String verifiedPayload(JwsVerifier verifier, String jws) {
        try {
            return verifier.verify(jws);
        } catch (JwsVerificationException e) {
            throw new VerifyException.SignatureInvalid("подпись документа не прошла проверку", e);
        }
    }

    private <T> T parse(String payload, Class<T> type) {
        try {
            return mapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new VerifyException.Malformed("подписанный документ нечитаем", e);
        }
    }
}
