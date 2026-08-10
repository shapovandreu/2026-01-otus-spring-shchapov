package ru.inversion.wharf.console.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements WebFilter {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'");

    private static final String[] STATIC_PREFIXES = {"/css/", "/js/"};

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");

        if (isStatic(exchange)) {
            headers.setCacheControl("no-cache");
        } else {
            headers.setCacheControl("no-store");
        }
        return chain.filter(exchange);
    }

    private static boolean isStatic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
