package ru.inversion.wharf.common.signing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

public class JwsSigner {

    private final RSAKey rsaKey;
    private final RSASSASigner signer;
    private final ObjectMapper objectMapper;

    public JwsSigner() {
        this.rsaKey = generateKey();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            this.signer = new RSASSASigner(rsaKey);
        } catch (JOSEException e) {
            throw new IllegalStateException("Не удалось создать RSA-подписывающего", e);
        }
    }

    public String sign(Object document) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build();
            JWSObject jws = new JWSObject(header, new Payload(objectMapper.writeValueAsString(document)));
            jws.sign(signer);
            return jws.serialize();
        } catch (JsonProcessingException | JOSEException e) {
            throw new IllegalStateException("Не удалось подписать документ", e);
        }
    }

    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("Не удалось извлечь публичный ключ", e);
        }
    }

    private static RSAKey generateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA недоступен в JVM", e);
        }
    }
}
