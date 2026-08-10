package ru.inversion.wharf.console.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.common.api.Roles;
import ru.inversion.wharf.console.client.ConsoleViews;
import ru.inversion.wharf.console.client.ControlPlaneClient;
import ru.inversion.wharf.console.client.ControlPlaneException;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.SecurityHeadersFilter;
import ru.inversion.wharf.console.security.SessionCookies;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {
        DashboardController.class,
        CatalogConsoleController.class,
        LicenseConsoleController.class,
        OrganizationConsoleController.class,
        AuditConsoleController.class,
        LoginController.class,
}, properties = {
        "iw.console.gateway-url=http://localhost:8080",
        "iw.console.request-timeout=5s",
        "iw.console.session.cookie-name=iw_operator",
        "iw.console.session.secure=false",
})
@Import({ConsoleAdvice.class, StaticAssets.class, ConsoleAuthFilter.class, SessionCookies.class,
        SecurityHeadersFilter.class})
class ConsoleRenderTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
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
    void rendersInstallationsTable() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        when(controlPlane.installations(anyString(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ConsoleViews.InstallationStatus(INSTALLATION, ORG, productId, "state_changed",
                        "RUNNING", releaseId, "установлено", Instant.parse("2026-07-21T10:00:00Z"))));
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.parse("2026-01-01T00:00:00Z"))));
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(productId, "wharf", null, Instant.now())));
        when(controlPlane.releases(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.ReleaseView(releaseId, productId, "1.4.2", "stable", true, null, Instant.now())));

        String html = get("/");

        assertThat(html).contains("Инсталляции");
        assertThat(html).contains("RUNNING").contains("state-ok");
        assertThat(html).contains("установлено");
        assertThat(html).contains("admin").contains("ADMIN");
        assertThat(html).contains("acme").contains("wharf").contains("1.4.2");
        assertThat(html).doesNotContain(productId.toString());
        assertThat(html).doesNotContain(releaseId.toString());
        assertThat(html).doesNotContain("<th>Инсталляция</th>");
        assertThat(html).contains("data-href=\"/installations/" + INSTALLATION + "\"");
    }

    @Test
    void rendersOneCellPerColumn() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.audit(anyString(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ConsoleViews.AuditView("1", "admin", List.of("ADMIN"), "publish-release",
                        "release", UUID.randomUUID().toString(), ORG, Instant.now())));

        assertCellsMatchHeaders(get("/orgs"), "/orgs");
        assertCellsMatchHeaders(get("/audit"), "/audit");
    }

    private static void assertCellsMatchHeaders(String html, String page) {
        java.util.regex.Matcher head = java.util.regex.Pattern
                .compile("<thead>(.*?)</thead>", java.util.regex.Pattern.DOTALL).matcher(html);
        java.util.regex.Matcher body = java.util.regex.Pattern
                .compile("<tbody>\\s*<tr>(.*?)</tr>", java.util.regex.Pattern.DOTALL).matcher(html);
        assertThat(head.find()).as("шапка таблицы на " + page).isTrue();
        assertThat(body.find()).as("строка данных на " + page).isTrue();

        assertThat(count(body.group(1), "<td"))
                .as("ячеек в строке данных на " + page)
                .isEqualTo(count(head.group(1), "<th"));
    }

    private static int count(String haystack, String needle) {
        int found = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    @Test
    void rendersEmptyStateInsteadOfBlankTable() throws Exception {
        when(controlPlane.installations(anyString(), any(), anyInt(), anyInt())).thenReturn(Flux.empty());
        when(controlPlane.organizations(anyString())).thenReturn(Flux.empty());
        when(controlPlane.products(anyString())).thenReturn(Flux.empty());

        assertThat(get("/")).contains("Телеметрии пока нет");
    }

    @Test
    void rendersControlPlaneFailureAsErrorPage() throws Exception {
        when(controlPlane.installations(anyString(), any(), anyInt(), anyInt())).thenReturn(
                Flux.error(new ControlPlaneException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                        "Control Plane недоступен")));
        when(controlPlane.organizations(anyString())).thenReturn(Flux.empty());
        when(controlPlane.products(anyString())).thenReturn(Flux.empty());

        String html = get("/");

        assertThat(html).contains("Операция не выполнена");
        assertThat(html).contains("Control Plane недоступен");
        assertThat(html).contains("SERVICE_UNAVAILABLE");
    }

    @Test
    void rendersLoginFormForAnonymousVisitor() {
        client.get().uri("/")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().location("/login");
    }

    @Test
    void setsSecurityHeadersOnPages() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));

        client.get().uri("/orgs")
                .cookie("iw_operator", jwt())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("Referrer-Policy", "no-referrer")
                .expectHeader().cacheControl(CacheControl.noStore())
                .expectHeader().value("Content-Security-Policy",
                        csp -> assertThat(csp)
                                .contains("default-src 'self'")
                                .contains("frame-ancestors 'none'")
                                .doesNotContain("unsafe-inline"));
    }

    @Test
    void revalidatesStaticInsteadOfCachingItForever() {
        client.get().uri("/js/console.js")
                .exchange()
                .expectHeader().cacheControl(CacheControl.noCache());
    }

    @Test
    void versionsStaticUrlsSoStaleCopiesCannotSurvive() {
        String html = client.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(html).isNotNull();
        assertThat(html).containsPattern("/js/console\\.js\\?v=[0-9a-f]+");
        assertThat(html).containsPattern("/css/console\\.css\\?v=[0-9a-f]+");
        assertThat(html).doesNotContain("\"/js/console.js\"");
    }

    @Test
    void setsSecurityHeadersOnLoginRedirect() {
        client.get().uri("/")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("X-Frame-Options", "DENY");
    }

    @Test
    void rendersEventsFeedWithReleaseVersion() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        when(controlPlane.events(anyString(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ConsoleViews.TelemetryView(ORG, UUID.randomUUID(), INSTALLATION, productId,
                        "state_changed", "FAILED", releaseId, "не удалось развернуть",
                        Instant.parse("2026-07-21T10:00:00Z"), Instant.parse("2026-07-21T10:00:05Z"))));
        when(controlPlane.releases(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.ReleaseView(releaseId, productId, "1.4.2", "stable", true, null, Instant.now())));

        String html = get("/installations/" + INSTALLATION);

        assertThat(html).contains("Лента событий");
        assertThat(html).contains("FAILED").contains("state-error");
        assertThat(html).contains("не удалось развернуть");
        assertThat(html).contains("1.4.2");
        assertThat(html).doesNotContain(releaseId.toString());
    }

    @Test
    void rendersEventsFeedWithUnknownReleaseAsId() throws Exception {
        UUID releaseId = UUID.randomUUID();
        when(controlPlane.events(anyString(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ConsoleViews.TelemetryView(ORG, UUID.randomUUID(), INSTALLATION, UUID.randomUUID(),
                        "state_changed", "RUNNING", releaseId, "установлено",
                        Instant.parse("2026-07-21T10:00:00Z"), Instant.parse("2026-07-21T10:00:05Z"))));
        when(controlPlane.releases(anyString(), any())).thenReturn(Flux.empty());

        assertThat(get("/installations/" + INSTALLATION)).contains(releaseId.toString());
    }

    @Test
    void rendersCatalogAndReleases() throws Exception {
        UUID productId = UUID.randomUUID();
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(productId, "wharf", "Платформа поставки", Instant.now())));
        when(controlPlane.releases(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.ReleaseView(UUID.randomUUID(), productId, "1.4.2", "stable", false,
                        "починили грейс", Instant.now()),
                new ConsoleViews.ReleaseView(UUID.randomUUID(), productId, "1.4.1", "beta", true,
                        null, Instant.now())));

        String catalog = get("/catalog");
        assertThat(catalog).contains("wharf").contains("Платформа поставки");
        assertThat(catalog).doesNotContain(productId.toString().substring(0, 8) + "…");
        assertThat(catalog).contains("data-href=\"/catalog/products/" + productId + "\"");

        String releases = get("/catalog/products/" + productId);
        assertThat(releases).contains("1.4.2").contains("черновик").contains("починили грейс");
        assertThat(releases).contains("1.4.1").contains("опубликован");
        assertThat(releases).containsOnlyOnce("Опубликовать релиз?");
        assertThat(releases).containsOnlyOnce("Сменить канал");
    }

    @Test
    void offersEditAndDeleteForDraftReleaseOnly() throws Exception {
        UUID productId = UUID.randomUUID();
        when(controlPlane.releases(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.ReleaseView(UUID.randomUUID(), productId, "1.4.2", "stable", false,
                        "починили грейс", Instant.now()),
                new ConsoleViews.ReleaseView(UUID.randomUUID(), productId, "1.4.1", "beta", true,
                        null, Instant.now())));

        String html = get("/catalog/products/" + productId);

        assertThat(html).containsOnlyOnce("Удалить черновик?");
        assertThat(html).containsOnlyOnce("data-dialog=\"editRelease\"");
    }

    @Test
    void offersEditAndDeleteForProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(productId, "wharf", "Платформа поставки", Instant.now())));

        String html = get("/catalog");

        assertThat(html).contains("data-dialog=\"editProduct\"");
        assertThat(html).contains("Удалить продукт?");
        assertThat(html).contains("/catalog/products/" + productId + "/delete");
    }

    @Test
    void rendersLicensesForSelectedOrganization() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(UUID.randomUUID(), "wharf", "Платформа поставки", Instant.now())));
        UUID productId = UUID.randomUUID();
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(productId, "wharf", "Платформа поставки", Instant.now())));
        when(controlPlane.entitlements(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.EntitlementView(UUID.randomUUID(), ORG, productId, "stable",
                        Instant.parse("2027-01-01T00:00:00Z"))));

        String html = get("/licenses?org=" + ORG);

        assertThat(html).contains("acme").contains("wharf").contains("stable");
        assertThat(html).contains("Изменить").contains("Отозвать");
        assertThat(html).doesNotContain("Выдать лицензию");
        assertThat(html).doesNotContain("Продлить");
    }

    @Test
    void rendersOpenEndedEntitlementExplicitly() throws Exception {
        UUID productId = UUID.randomUUID();
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.products(anyString())).thenReturn(Flux.just(
                new ConsoleViews.ProductView(productId, "wharf", null, Instant.now())));
        when(controlPlane.entitlements(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.EntitlementView(UUID.randomUUID(), ORG, productId, "beta", null)));

        assertThat(get("/licenses")).contains("бессрочно");
    }

    @Test
    void showsIssuedEnrollmentSecretOnce() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.enrollmentTokens(anyString(), any())).thenReturn(Flux.empty());
        when(controlPlane.issueEnrollmentToken(anyString(), any(), any())).thenReturn(Mono.just(
                new ConsoleViews.IssuedTokenView(UUID.randomUUID(), ORG, "секрет-одноразовый",
                        Instant.parse("2026-07-22T10:00:00Z"))));

        byte[] body = client.post().uri("/licenses/tokens")
                .cookie("iw_operator", jwt())
                .body(BodyInserters
                        .fromFormData("orgId", ORG.toString()).with("ttlHours", "24"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        String html = body == null ? "" : new String(body, StandardCharsets.UTF_8);

        assertThat(html).contains("секрет-одноразовый");
        assertThat(html).contains("единственный показ");
    }

    @Test
    void rendersAuditJournalWithRoles() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.audit(anyString(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.just(
                new ConsoleViews.AuditView("1", "admin", List.of("ADMIN", "RM"), "publish-release",
                        "release", UUID.randomUUID().toString(), ORG, Instant.parse("2026-07-21T09:00:00Z"))));

        String html = get("/audit");

        assertThat(html).contains("Журнал операторских действий");
        assertThat(html).contains("publish-release");
        assertThat(html).contains("ADMIN, RM");
        assertThat(html).contains("<td>acme</td>");
        assertThat(html).doesNotContain("<td>" + ORG + "</td>");
    }

    @Test
    void rendersOrganizations() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.parse("2026-01-01T00:00:00Z"))));

        String html = get("/orgs");
        assertThat(html).contains("acme").contains("Новая организация");
        assertThat(html).doesNotContain("class=\"uuid\"");
        assertThat(html).contains("Переименовать").contains("Удалить");
    }

    @Test
    void rendersEnrollmentTokenListWithoutSecrets() throws Exception {
        when(controlPlane.organizations(anyString())).thenReturn(Flux.just(
                new ConsoleViews.OrgView(ORG, "acme", Instant.now())));
        when(controlPlane.enrollmentTokens(anyString(), any())).thenReturn(Flux.just(
                new ConsoleViews.TokenSummaryView(UUID.randomUUID(), ORG, "ACTIVE",
                        Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-07-30T00:00:00Z")),
                new ConsoleViews.TokenSummaryView(UUID.randomUUID(), ORG, "USED",
                        Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-07-29T00:00:00Z"))));

        String html = get("/licenses/tokens");

        assertThat(html).contains("ACTIVE").contains("USED").contains("acme");
        assertThat(html).containsOnlyOnce("Отозвать этот токен?");
        assertThat(html).doesNotContain("единственный показ");
    }

    @Test
    void rendersLoginForm() {
        byte[] body = client.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        String html = body == null ? "" : new String(body, StandardCharsets.UTF_8);

        assertThat(html).contains("Вход оператора");
        assertThat(html).doesNotContain("выйти");
    }

    private String get(String uri) throws Exception {
        byte[] body = client.get().uri(uri)
                .cookie("iw_operator", jwt())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static String jwt() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin")
                .claim(Roles.CLAIM_ROLES, List.of(Roles.ADMIN))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key.toPrivateKey()));
        return jwt.serialize();
    }
}
