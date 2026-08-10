package ru.inversion.wharf.console.web;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component("staticAssets")
public class StaticAssets {

    private static final List<String> VERSIONED = List.of("static/css/console.css", "static/js/console.js");

    private final String version;

    public StaticAssets() {
        this.version = digest();
    }

    public String version() {
        return version;
    }

    private static String digest() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (String path : VERSIONED) {
                try (InputStream in = new ClassPathResource(path).getInputStream()) {
                    sha.update(FileCopyUtils.copyToByteArray(in));
                }
            }
            String hex = new BigInteger(1, sha.digest()).toString(16);
            return hex.length() > 8 ? hex.substring(0, 8) : hex;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Не удалось посчитать версию статики консоли", e);
        }
    }
}
