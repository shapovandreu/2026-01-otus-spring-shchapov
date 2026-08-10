package ru.inversion.wharf.console.web;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ru.inversion.wharf.console.client.ConsoleViews.IssuedTokenView;
import ru.inversion.wharf.console.client.ControlPlaneClient;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.OperatorSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/licenses")
public class LicenseConsoleController {

    private static final Set<String> CHANNELS = Set.of("stable", "beta");

    private final ControlPlaneClient controlPlane;

    public LicenseConsoleController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping
    public Mono<Rendering> overview(@RequestParam(required = false) UUID org, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return Mono.zip(
                        controlPlane.organizations(session.token()).collectList(),
                        controlPlane.products(session.token()).collectList(),
                        controlPlane.entitlements(session.token(), org).collectList())
                .map(loaded -> {
                    Map<String, Object> model = new LinkedHashMap<>();
                    model.put("orgs", loaded.getT1());
                    model.put("products", loaded.getT2());
                    model.put("entitlements", loaded.getT3());
                    model.put("orgNames", loaded.getT1().stream()
                            .collect(Collectors.toMap(o -> o.id(), o -> o.name())));
                    model.put("productNames", loaded.getT2().stream()
                            .collect(Collectors.toMap(p -> p.id(), p -> p.name())));
                    if (org != null) {
                        model.put("selectedOrg", org);
                    }
                    return Rendering.view("licenses").model(model).build();
                });
    }

    @PostMapping("/entitlements")
    public Mono<Rendering> grant(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> {
                    UUID orgId = form.uuid("orgId");
                    return controlPlane.grantEntitlement(session.token(), orgId,
                                    form.uuid("productId"),
                                    form.choice("channel", CHANNELS, "stable"),
                                    validUntil(form))
                            .thenReturn(orgId);
                })
                .map(orgId -> Rendering.redirectTo("/licenses?org=" + orgId).build());
    }

    @PostMapping("/entitlements/{entitlementId}")
    public Mono<Rendering> update(@PathVariable UUID entitlementId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.updateEntitlement(session.token(), entitlementId, validUntil(form)))
                .map(entitlement -> Rendering.redirectTo("/licenses?org=" + entitlement.orgId()).build());
    }

    @PostMapping("/entitlements/{entitlementId}/revoke")
    public Mono<Rendering> revoke(@PathVariable UUID entitlementId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> {
                    UUID orgId = form.optionalUuid("orgId");
                    return controlPlane.revokeEntitlement(session.token(), entitlementId)
                            .thenReturn(orgId == null ? "/licenses" : "/licenses?org=" + orgId);
                })
                .map(target -> Rendering.redirectTo(target).build());
    }

    @GetMapping("/tokens")
    public Mono<Rendering> tokens(@RequestParam(required = false) UUID org, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return tokensPage(session, org, null);
    }

    @PostMapping("/tokens")
    public Mono<Rendering> issueToken(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> {
                    Duration ttl = Duration.ofHours(form.number("ttlHours", 24));
                    return controlPlane.issueEnrollmentToken(session.token(), form.uuid("orgId"), ttl);
                })
                .flatMap(issued -> tokensPage(session, null, issued));
    }

    @PostMapping("/tokens/{tokenId}/revoke")
    public Mono<Rendering> revokeToken(@PathVariable UUID tokenId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.revokeEnrollmentToken(session.token(), tokenId)
                .thenReturn(Rendering.redirectTo("/licenses/tokens").build());
    }

    private Mono<Rendering> tokensPage(OperatorSession session, UUID org, IssuedTokenView issued) {
        return Mono.zip(
                        controlPlane.organizations(session.token()).collectList(),
                        controlPlane.enrollmentTokens(session.token(), org).collectList())
                .map(loaded -> {
                    Map<String, Object> model = new LinkedHashMap<>();
                    model.put("orgs", loaded.getT1());
                    model.put("tokens", loaded.getT2());
                    model.put("orgNames", loaded.getT1().stream()
                            .collect(Collectors.toMap(o -> o.id(), o -> o.name())));
                    if (org != null) {
                        model.put("selectedOrg", org);
                    }
                    if (issued != null) {
                        model.put("issued", issued);
                    }
                    return Rendering.view("tokens").model(model).build();
                });
    }

    private static Instant validUntil(Forms form) {
        long days = form.number("validDays", 0);
        return days <= 0 ? null : Instant.now().plus(Duration.ofDays(days));
    }
}
