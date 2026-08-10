package ru.inversion.wharf.agent.verify;

public class VerifyException extends RuntimeException {

    public VerifyException(String message) {
        super(message);
    }

    public VerifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class SignatureInvalid extends VerifyException {
        public SignatureInvalid(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class Malformed extends VerifyException {
        public Malformed(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
