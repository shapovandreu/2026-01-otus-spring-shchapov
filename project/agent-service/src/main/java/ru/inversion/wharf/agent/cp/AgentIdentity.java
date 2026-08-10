package ru.inversion.wharf.agent.cp;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class AgentIdentity {

    private final AtomicReference<Token> token = new AtomicReference<>();

    public record Token(String accessToken, Instant expiresAt) {
    }

    public void assign(Token newToken) {
        token.set(newToken);
    }

    public boolean enrolled() {
        return token.get() != null;
    }

    public Optional<Token> current() {
        return Optional.ofNullable(token.get());
    }

    public String bearer() {
        Token current = token.get();
        if (current == null) {
            throw new IllegalStateException("Агент не зарегистрирован: сначала enrollment");
        }
        return current.accessToken();
    }
}
