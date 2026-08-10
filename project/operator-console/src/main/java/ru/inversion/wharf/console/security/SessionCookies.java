package ru.inversion.wharf.console.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import ru.inversion.wharf.console.config.ConsoleProperties;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class SessionCookies {

    private final ConsoleProperties properties;

    public SessionCookies(ConsoleProperties properties) {
        this.properties = properties;
    }

    public Optional<String> read(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(properties.session().cookieName());
        return cookie == null || cookie.getValue().isBlank() ? Optional.empty() : Optional.of(cookie.getValue());
    }

    public void write(ServerWebExchange exchange, String token, Instant expiresAt) {
        Duration maxAge = expiresAt == null
                ? Duration.ofHours(1)
                : Duration.between(Instant.now(), expiresAt);
        exchange.getResponse().addCookie(base(token)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build());
    }

    public void clear(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(base("").maxAge(Duration.ZERO).build());
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.session().cookieName(), value)
                .httpOnly(true)
                .secure(properties.session().secure())
                .sameSite("Strict")
                .path("/");
    }
}
