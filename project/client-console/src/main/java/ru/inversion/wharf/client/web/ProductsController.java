package ru.inversion.wharf.client.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ru.inversion.wharf.client.client.ClientViews.InstallationStatus;
import ru.inversion.wharf.client.client.ControlPlaneClient;
import ru.inversion.wharf.client.security.ClientAuthFilter;
import ru.inversion.wharf.client.security.ClientSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class ProductsController {

    private static final Set<String> ACTIONS = Set.of("INSTALL", "UPDATE", "UNINSTALL");

    private static final int PAGE_SIZE = 200;

    private final ControlPlaneClient controlPlane;

    public ProductsController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping("/")
    public Mono<Rendering> products(ServerWebExchange exchange) {
        ClientSession session = ClientAuthFilter.require(exchange);
        return Mono.zip(
                        controlPlane.availableProducts(session.token()).collectList(),
                        controlPlane.installations(session.token(), 0, PAGE_SIZE).collectList(),
                        controlPlane.pendingIntents(session.token()).collectList())
                .map(loaded -> {
                    Map<String, Object> model = new LinkedHashMap<>();
                    model.put("products", loaded.getT1());
                    model.put("installations", loaded.getT2());
                    model.put("pending", loaded.getT3());
                    model.put("installedByProduct", byProduct(loaded.getT2()));
                    model.put("pendingByProduct", loaded.getT3().stream()
                            .collect(Collectors.toMap(intent -> intent.productId(),
                                    intent -> intent.action(), (first, second) -> first)));
                    return Rendering.view("products").model(model).build();
                });
    }

    @PostMapping("/intents")
    public Mono<Rendering> submit(ServerWebExchange exchange) {
        ClientSession session = ClientAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> {
                    String action = form.choice("action", ACTIONS, "INSTALL");
                    UUID installationId = form.optionalUuid("installationId");
                    return controlPlane.submitIntent(session.token(),
                            installationId == null ? UUID.randomUUID() : installationId,
                            form.uuid("productId"),
                            action,
                            "UNINSTALL".equals(action) ? null : form.uuid("releaseId"));
                })
                .thenReturn(Rendering.redirectTo("/").build());
    }

    private static Map<UUID, InstallationStatus> byProduct(java.util.List<InstallationStatus> installations) {
        Map<UUID, InstallationStatus> latest = new LinkedHashMap<>();
        for (InstallationStatus status : installations) {
            latest.merge(status.productId(), status,
                    (existing, candidate) -> candidate.lastSeen() == null || existing.lastSeen() == null
                            ? existing
                            : candidate.lastSeen().isAfter(existing.lastSeen()) ? candidate : existing);
        }
        return latest;
    }
}
