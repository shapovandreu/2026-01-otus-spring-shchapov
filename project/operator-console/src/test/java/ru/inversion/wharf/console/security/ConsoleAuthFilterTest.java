package ru.inversion.wharf.console.security;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.console.config.ConsoleProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleAuthFilterTest {

    private static final String COOKIE = "iw_operator";

    private static RSAKey key;

    private final ConsoleProperties properties = new ConsoleProperties(
            "http://localhost:8080", Duration.ofSeconds(5), new ConsoleProperties.Session(COOKIE, false));
    private final ConsoleAuthFilter filter = new ConsoleAuthFilter(new SessionCookies(properties));

    @BeforeAll
    static void generateKey() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test").generate();
    }

    @Test
    void sendsAnonymousVisitorToPlainLogin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        AtomicBoolean reachedController = new AtomicBoolean();

        filter.filter(exchange, chain(reachedController)).block();

        assertThat(reachedController).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("/login");
        assertThat(exchange.getResponse().getCookies().getFirst(COOKIE)).isNull();
    }

    @Test
    void unreadableCookieIsReportedAsExpired() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").cookie(new HttpCookie(COOKIE, "мусор")));
        AtomicBoolean reachedController = new AtomicBoolean();

        filter.filter(exchange, chain(reachedController)).block();

        assertThat(reachedController).isFalse();
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("/login?expired");
        assertThat(exchange.getResponse().getCookies().getFirst(COOKIE)).isNotNull();
    }

    @Test
    void letsLoginPageThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login"));
        AtomicBoolean reachedController = new AtomicBoolean();

        filter.filter(exchange, chain(reachedController)).block();

        assertThat(reachedController).isTrue();
    }

    @Test
    void letsStaticAssetsThrough() {
        for (String asset : List.of("/css/console.css", "/js/console.js")) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(asset));
            AtomicBoolean reachedController = new AtomicBoolean();

            filter.filter(exchange, chain(reachedController)).block();

            assertThat(reachedController).as(asset).isTrue();
        }
    }

    @Test
    void publishesSessionForValidCookie() throws Exception {
        MockServerWebExchange exchange = exchangeWithToken(jwt(Duration.ofHours(1)));
        AtomicBoolean reachedController = new AtomicBoolean();

        filter.filter(exchange, chain(reachedController)).block();

        assertThat(reachedController).isTrue();
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        assertThat(session.username()).isEqualTo("admin");
        assertThat(session.canManageOrganizations()).isTrue();
    }

    @Test
    void expiredCookieIsClearedAndRedirected() throws Exception {
        MockServerWebExchange exchange = exchangeWithToken(jwt(Duration.ofMinutes(-1)));
        AtomicBoolean reachedController = new AtomicBoolean();

        filter.filter(exchange, chain(reachedController)).block();

        assertThat(reachedController).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("/login?expired");
        ResponseCookie cleared = exchange.getResponse().getCookies().getFirst(COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    private static MockServerWebExchange exchangeWithToken(String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/").cookie(new HttpCookie(COOKIE, token)));
    }

    private static WebFilterChain chain(AtomicBoolean reached) {
        return (ServerWebExchange exchange) -> {
            reached.set(true);
            return Mono.empty();
        };
    }

    private static String jwt(Duration ttl) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin")
                .claim(Roles.CLAIM_ROLES, List.of(Roles.ADMIN))
                .expirationTime(Date.from(java.time.Instant.now().plus(ttl)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }
}
