package ru.inversion.wharf.console.web;

import java.util.Set;
import java.util.UUID;

import ru.inversion.wharf.console.client.ControlPlaneClient;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.OperatorSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/catalog")
public class CatalogConsoleController {

    private static final Set<String> CHANNELS = Set.of("STABLE", "BETA");

    private final ControlPlaneClient controlPlane;

    public CatalogConsoleController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping
    public Mono<Rendering> products(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.products(session.token())
                .collectList()
                .map(products -> Rendering.view("catalog")
                        .modelAttribute("products", products)
                        .build());
    }

    @PostMapping("/products")
    public Mono<Rendering> createProduct(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.createProduct(session.token(),
                        form.text("name"), form.optionalText("description")))
                .thenReturn(Rendering.redirectTo("/catalog").build());
    }

    @PostMapping("/products/{productId}")
    public Mono<Rendering> updateProduct(@PathVariable UUID productId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.updateProduct(session.token(), productId,
                        form.text("name"), form.optionalText("description")))
                .thenReturn(Rendering.redirectTo("/catalog").build());
    }

    @PostMapping("/products/{productId}/delete")
    public Mono<Rendering> deleteProduct(@PathVariable UUID productId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.deleteProduct(session.token(), productId)
                .thenReturn(Rendering.redirectTo("/catalog").build());
    }

    @GetMapping("/products/{productId}")
    public Mono<Rendering> releases(@PathVariable UUID productId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.releases(session.token(), productId)
                .collectList()
                .map(releases -> Rendering.view("releases")
                        .modelAttribute("releases", releases)
                        .modelAttribute("productId", productId)
                        .build());
    }

    @PostMapping("/products/{productId}/releases")
    public Mono<Rendering> createRelease(@PathVariable UUID productId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.createRelease(session.token(), productId,
                        form.text("version"),
                        form.choice("channel", CHANNELS, "STABLE"),
                        form.optionalText("changelog")))
                .thenReturn(Rendering.redirectTo("/catalog/products/" + productId).build());
    }

    @PostMapping("/releases/{releaseId}/publish")
    public Mono<Rendering> publish(@PathVariable UUID releaseId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.publishRelease(session.token(), releaseId)
                .map(release -> Rendering.redirectTo("/catalog/products/" + release.productId()).build());
    }

    @PostMapping("/releases/{releaseId}")
    public Mono<Rendering> updateRelease(@PathVariable UUID releaseId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.updateRelease(session.token(), releaseId,
                        form.text("version"),
                        form.choice("channel", CHANNELS, "STABLE"),
                        form.optionalText("changelog")))
                .map(release -> Rendering.redirectTo("/catalog/products/" + release.productId()).build());
    }

    @PostMapping("/releases/{releaseId}/delete")
    public Mono<Rendering> deleteRelease(@PathVariable UUID releaseId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> {
                    UUID productId = form.optionalUuid("productId");
                    return controlPlane.deleteRelease(session.token(), releaseId)
                            .thenReturn(productId == null ? "/catalog" : "/catalog/products/" + productId);
                })
                .map(target -> Rendering.redirectTo(target).build());
    }

    @PostMapping("/releases/{releaseId}/channel")
    public Mono<Rendering> changeChannel(@PathVariable UUID releaseId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.changeReleaseChannel(session.token(), releaseId,
                        form.choice("channel", CHANNELS, "STABLE")))
                .map(release -> Rendering.redirectTo("/catalog/products/" + release.productId()).build());
    }
}
