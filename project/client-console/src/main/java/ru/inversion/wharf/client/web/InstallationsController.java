package ru.inversion.wharf.client.web;

import java.util.UUID;

import ru.inversion.wharf.client.client.ControlPlaneClient;
import ru.inversion.wharf.client.security.ClientAuthFilter;
import ru.inversion.wharf.client.security.ClientSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class InstallationsController {

    private static final int PAGE_SIZE = 50;

    private final ControlPlaneClient controlPlane;

    public InstallationsController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping("/installations")
    public Mono<Rendering> installations(@RequestParam(defaultValue = "0") int page,
                                         ServerWebExchange exchange) {
        ClientSession session = ClientAuthFilter.require(exchange);
        return controlPlane.installations(session.token(), page, PAGE_SIZE)
                .collectList()
                .map(installations -> Rendering.view("installations")
                        .modelAttribute("installations", installations)
                        .modelAttribute("page", page)
                        .modelAttribute("baseUrl", "/installations")
                        .modelAttribute("hasNext", installations.size() == PAGE_SIZE)
                        .build());
    }

    @GetMapping("/installations/{installationId}")
    public Mono<Rendering> events(@PathVariable UUID installationId,
                                  @RequestParam(defaultValue = "0") int page,
                                  ServerWebExchange exchange) {
        ClientSession session = ClientAuthFilter.require(exchange);
        return controlPlane.events(session.token(), installationId, page, PAGE_SIZE)
                .collectList()
                .map(events -> Rendering.view("events")
                        .modelAttribute("events", events)
                        .modelAttribute("installationId", installationId)
                        .modelAttribute("page", page)
                        .modelAttribute("baseUrl", "/installations/" + installationId)
                        .modelAttribute("hasNext", events.size() == PAGE_SIZE)
                        .build());
    }
}
