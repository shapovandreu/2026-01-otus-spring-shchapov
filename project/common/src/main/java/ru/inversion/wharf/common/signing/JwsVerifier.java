package ru.inversion.wharf.common.signing;

import java.text.ParseException;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;

public final class JwsVerifier {

    private final JWKSet jwks;

    public JwsVerifier(JWKSet jwks) {
        this.jwks = jwks;
    }

    public static JwsVerifier fromJwksJson(String jwksJson) {
        try {
            return new JwsVerifier(JWKSet.parse(jwksJson));
        } catch (ParseException e) {
            throw new JwsVerificationException("Не удалось разобрать JWKS", e);
        }
    }

    public String verify(String compactJws) {
        JWSObject jws = parse(compactJws);
        RSAKey key = selectKey(jws.getHeader().getKeyID());
        try {
            if (!jws.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                throw new JwsVerificationException("Подпись документа не совпала с ключом издателя");
            }
        } catch (JOSEException e) {
            throw new JwsVerificationException("Ошибка проверки подписи", e);
        }
        return jws.getPayload().toString();
    }

    private static JWSObject parse(String compactJws) {
        try {
            return JWSObject.parse(compactJws);
        } catch (ParseException e) {
            throw new JwsVerificationException("Документ не является компактным JWS", e);
        }
    }

    private RSAKey selectKey(String keyId) {
        List<RSAKey> rsaKeys = jwks.getKeys().stream()
                .filter(key -> key.getKeyType() == KeyType.RSA)
                .map(RSAKey.class::cast)
                .toList();
        if (rsaKeys.isEmpty()) {
            throw new JwsVerificationException("В JWKS нет RSA-ключей");
        }
        if (keyId != null) {
            return rsaKeys.stream()
                    .filter(key -> keyId.equals(key.getKeyID()))
                    .findFirst()
                    .orElseThrow(() -> new JwsVerificationException("В JWKS нет ключа с kid " + keyId));
        }
        if (rsaKeys.size() != 1) {
            throw new JwsVerificationException("JWS без kid, а в JWKS несколько ключей");
        }
        return rsaKeys.getFirst();
    }
}
