package ru.inversion.wharf.client.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.client.client.ClientViews;
import ru.inversion.wharf.client.client.ControlPlaneClient;
import ru.inversion.wharf.client.client.ControlPlaneException;
import ru.inversion.wharf.client.security.ClientAuthFilter;
import ru.inversion.wharf.client.security.SecurityHeadersFilter;
import ru.inversion.wharf.client.security.SessionCookies;
import ru.inversion.wharf.common.api.Roles;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {
        ProductsController.class,
        InstallationsController.class,
        LoginController.class,
}, properties = {
        "iw.client-console.gateway-url=http://localhost:8080",
        "iw.client-console.request-timeout=5s",
        "iw.client-console.session.cookie-name=iw_client",
        "iw.client-console.session.secure=false",
})
@Import({ClientAdvice.class, StaticAssets.class, ClientAuthFilter.class, SessionCookies.class,
        SecurityHeadersFilter.class})
class ClientConsoleRenderTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RELEASE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INSTALLATION = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static RSAKey key;

    @MockitoBean
    private ControlPlaneClient controlPlane;

    @Autowired
    private WebTestClient client;

    @BeforeAll
    static void generateKey() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test").generate();
    }

    @Test
    void rendersAvailableProductWithInstallButton() throws Exception {
        availableProduct();
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.empty());

        String html = get("/");

        assertThat(html).contains("wharf").contains("Платформа поставки").contains("1.4.2");
        assertThat(html).contains("не установлен");
        assertThat(html).contains("Установить");
        assertThat(html).doesNotContain("Удалить");
    }

    @Test
    void offersUpdateAndRemoveForRunningProduct() throws Exception {
        availableProduct();
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ClientViews.InstallationStatus(INSTALLATION, ORG, PRODUCT, "state_changed",
                        "RUNNING", RELEASE, "installation running", Instant.parse("2026-07-21T10:00:00Z"))));
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.empty());

        String html = get("/");

        assertThat(html).contains("Обновить").contains("Удалить");
        assertThat(html).doesNotContain(">Установить<");
        assertThat(html).contains(INSTALLATION.toString());
    }

    @Test
    void showsQueuedIntentBeforeAgentExecutesIt() throws Exception {
        availableProduct();
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.just(
                new ClientViews.IntentView(UUID.randomUUID(), INSTALLATION, PRODUCT, "install",
                        RELEASE, "pending", Instant.parse("2026-07-21T10:00:00Z"))));

        String html = get("/");

        assertThat(html).contains("в очереди").contains("Ожидают выполнения агентом");
    }

    @Test
    void hidesActionsWhenChannelHasNoReleases() throws Exception {
        when(controlPlane.availableProducts(anyString())).thenReturn(Flux.just(
                new ClientViews.AvailableProductView(PRODUCT, "wharf", null, "beta",
                        null, List.of(), null)));
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.empty());

        String html = get("/");

        assertThat(html).contains("нет опубликованных релизов");
        assertThat(html).doesNotContain("Установить");
    }

    @Test
    void rendersEmptyStateWhenNoEntitlements() throws Exception {
        when(controlPlane.availableProducts(anyString())).thenReturn(Flux.empty());
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.empty());

        assertThat(get("/")).contains("не выдано ни одного права");
    }

    @Test
    void rendersOwnInstallations() throws Exception {
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ClientViews.InstallationStatus(INSTALLATION, ORG, PRODUCT, "state_changed",
                        "REMOVED", RELEASE, "installation removed", Instant.parse("2026-07-21T10:00:00Z"))));

        String html = get("/installations");

        assertThat(html).contains("Мои инсталляции").contains("REMOVED");
    }

    @Test
    void rendersEventsFeed() throws Exception {
        when(controlPlane.events(anyString(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ClientViews.TelemetryView(ORG, UUID.randomUUID(), INSTALLATION, PRODUCT,
                        "state_changed", "RUNNING", RELEASE, "installation running",
                        Instant.parse("2026-07-21T10:00:00Z"), Instant.parse("2026-07-21T10:00:05Z"))));

        String html = get("/installations/" + INSTALLATION);

        assertThat(html).contains("Лента событий").contains("RUNNING").contains("state-ok");
    }

    @Test
    void rendersControlPlaneFailureAsErrorPage() throws Exception {
        when(controlPlane.availableProducts(anyString())).thenReturn(Flux.error(
                new ControlPlaneException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                        "Control Plane недоступен")));
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.pendingIntents(anyString())).thenReturn(Flux.empty());

        String html = get("/");

        assertThat(html).contains("Control Plane недоступен").contains("SERVICE_UNAVAILABLE");
    }

    @Test
    void sendsAnonymousVisitorToLogin() {
        client.get().uri("/")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().location("/login");
    }

    @Test
    void rejectsVendorOperatorToken() throws Exception {
        client.get().uri("/")
                .cookie("iw_client", vendorJwt())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().location("/login?expired");
    }

    @Test
    void setsSecurityHeadersAndVersionsStatic() throws Exception {
        when(controlPlane.installations(anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());

        String html = client.get().uri("/installations")
                .cookie("iw_client", jwt())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().value("Content-Security-Policy",
                        csp -> assertThat(csp).doesNotContain("unsafe-inline"))
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(html).isNotNull();
        assertThat(html).containsPattern("/js/console\\.js\\?v=[0-9a-f]+");
    }

    @Test
    void rendersLoginForm() {
        byte[] body = client.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        String html = body == null ? "" : new String(body, StandardCharsets.UTF_8);

        assertThat(html).contains("Вход администратора организации");
        assertThat(html).doesNotContain("выйти");
    }

    private void availableProduct() {
        when(controlPlane.availableProducts(anyString())).thenReturn(Flux.just(
                new ClientViews.AvailableProductView(PRODUCT, "wharf", "Платформа поставки", "stable",
                        null,
                        List.of(new ClientViews.ReleaseView(RELEASE, PRODUCT, "1.4.2", "stable", true,
                                "починили грейс", Instant.parse("2026-07-20T10:00:00Z"))),
                        RELEASE)));
    }

    private String get(String uri) throws Exception {
        byte[] body = client.get().uri(uri)
                .cookie("iw_client", jwt())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static String jwt() throws Exception {
        return token("acme-admin", List.of(Roles.ORG_ADMIN), ORG);
    }

    private static String vendorJwt() throws Exception {
        return token("admin", List.of(Roles.ADMIN), null);
    }

    private static String token(String subject, List<String> roles, UUID orgId) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim(Roles.CLAIM_ROLES, roles)
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
        if (orgId != null) {
            claims.claim(Roles.CLAIM_ORG, orgId.toString());
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }
}
