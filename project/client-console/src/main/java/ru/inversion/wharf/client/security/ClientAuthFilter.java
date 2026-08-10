package ru.inversion.wharf.client.security;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ClientAuthFilter implements WebFilter {

    public static final String SESSION_ATTRIBUTE = "iw.client-session";

    private static final Set<String> ANONYMOUS_PATHS = Set.of("/login", "/logout", "/favicon.ico");
    private static final Set<String> STATIC_PREFIXES = Set.of("/css/", "/js/");
    private static final String ACTUATOR_PREFIX = "/actuator";

    private final SessionCookies cookies;

    public ClientAuthFilter(SessionCookies cookies) {
        this.cookies = cookies;
    }

    public static ClientSession require(ServerWebExchange exchange) {
        ClientSession session = exchange.getAttribute(SESSION_ATTRIBUTE);
        if (session == null) {
            throw new IllegalStateException("Экран доступен без сессии — путь ошибочно помечен анонимным");
        }
        return session;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Optional<String> cookie = cookies.read(exchange);
        Optional<ClientSession> session = cookie
                .flatMap(ClientSession::parse)
                .filter(parsed -> !parsed.isExpired(Instant.now()))
                .filter(ClientSession::isOrgAdmin);

        session.ifPresent(parsed -> exchange.getAttributes().put(SESSION_ATTRIBUTE, parsed));

        if (session.isPresent() || isAnonymous(exchange)) {
            return chain.filter(exchange);
        }
        return redirectToLogin(exchange, cookie.isPresent());
    }

    private boolean isAnonymous(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return ANONYMOUS_PATHS.contains(path)
                || path.startsWith(ACTUATOR_PREFIX)
                || STATIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> redirectToLogin(ServerWebExchange exchange, boolean hadCookie) {
        if (hadCookie) {
            cookies.clear(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
        exchange.getResponse().getHeaders().setLocation(URI.create(hadCookie ? "/login?expired" : "/login"));
        return exchange.getResponse().setComplete();
    }
}
