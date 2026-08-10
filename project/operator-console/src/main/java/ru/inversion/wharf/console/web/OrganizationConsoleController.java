package ru.inversion.wharf.console.web;

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
@RequestMapping("/orgs")
public class OrganizationConsoleController {

    private final ControlPlaneClient controlPlane;

    public OrganizationConsoleController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping
    public Mono<Rendering> list(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.organizations(session.token())
                .collectList()
                .map(orgs -> Rendering.view("orgs").modelAttribute("orgs", orgs).build());
    }

    @PostMapping
    public Mono<Rendering> create(ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.createOrganization(session.token(), form.text("name")))
                .thenReturn(Rendering.redirectTo("/orgs").build());
    }

    @PostMapping("/{orgId}")
    public Mono<Rendering> rename(@PathVariable UUID orgId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.renameOrganization(session.token(), orgId, form.text("name")))
                .thenReturn(Rendering.redirectTo("/orgs").build());
    }

    @PostMapping("/{orgId}/delete")
    public Mono<Rendering> delete(@PathVariable UUID orgId, ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.deleteOrganization(session.token(), orgId)
                .thenReturn(Rendering.redirectTo("/orgs").build());
    }
}
