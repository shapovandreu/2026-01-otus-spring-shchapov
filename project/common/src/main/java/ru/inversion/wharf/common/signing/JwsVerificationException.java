package ru.inversion.wharf.common.signing;

public class JwsVerificationException extends RuntimeException {

    public JwsVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwsVerificationException(String message) {
        super(message);
    }
}
