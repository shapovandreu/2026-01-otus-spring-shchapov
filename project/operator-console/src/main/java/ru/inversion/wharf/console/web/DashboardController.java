package ru.inversion.wharf.console.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import ru.inversion.wharf.console.client.ConsoleViews.InstallationStatus;
import ru.inversion.wharf.console.client.ConsoleViews.OrgView;
import ru.inversion.wharf.console.client.ConsoleViews.ProductView;
import ru.inversion.wharf.console.client.ConsoleViews.ReleaseView;
import ru.inversion.wharf.console.client.ConsoleViews.TelemetryView;
import ru.inversion.wharf.console.client.ControlPlaneClient;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.OperatorSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class DashboardController {

    private static final int PAGE_SIZE = 50;

    private final ControlPlaneClient controlPlane;

    public DashboardController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping("/")
    public Mono<Rendering> installations(@RequestParam(required = false) UUID org,
                                         @RequestParam(defaultValue = "0") int page,
                                         ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return Mono.zip(
                        controlPlane.installations(session.token(), org, page, PAGE_SIZE).collectList(),
                        controlPlane.organizations(session.token()).collectList(),
                        controlPlane.products(session.token()).collectList())
                .flatMap(loaded -> {
                    List<InstallationStatus> installations = loaded.getT1();
                    return releaseVersions(session.token(), installations.stream()
                            .map(InstallationStatus::productId))
                            .map(versions -> {
                                Map<String, Object> model = new LinkedHashMap<>();
                                model.put("installations", installations);
                                model.put("orgs", loaded.getT2());
                                model.put("selectedOrg", org);
                                model.put("orgNames", namesOf(loaded.getT2(), OrgView::id, OrgView::name));
                                model.put("productNames",
                                        namesOf(loaded.getT3(), ProductView::id, ProductView::name));
                                model.put("releaseVersions", versions);
                                model.put("page", page);
                                model.put("baseUrl", org == null ? "/" : "/?org=" + org);
                                model.put("hasNext", installations.size() == PAGE_SIZE);
                                return Rendering.view("installations").model(model).build();
                            });
                });
    }

    @GetMapping("/installations/{installationId}")
    public Mono<Rendering> events(@PathVariable UUID installationId,
                                  @RequestParam(defaultValue = "0") int page,
                                  ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return controlPlane.events(session.token(), installationId, null, page, PAGE_SIZE)
                .collectList()
                .flatMap(events -> releaseVersions(session.token(), events.stream()
                        .map(TelemetryView::productId))
                        .map(versions -> {
                            Map<String, Object> model = new LinkedHashMap<>();
                            model.put("events", events);
                            model.put("installationId", installationId);
                            model.put("releaseVersions", versions);
                            model.put("page", page);
                            model.put("baseUrl", "/installations/" + installationId);
                            model.put("hasNext", events.size() == PAGE_SIZE);
                            return Rendering.view("events").model(model).build();
                        }));
    }

    private Mono<Map<UUID, String>> releaseVersions(String token, java.util.stream.Stream<UUID> productIds) {
        List<UUID> products = productIds
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return Flux.fromIterable(products)
                .concatMap(productId -> controlPlane.releases(token, productId))
                .collectList()
                .map(releases -> namesOf(releases, ReleaseView::id, ReleaseView::version));
    }

    private static <T> Map<UUID, String> namesOf(List<T> items, Function<T, UUID> id, Function<T, String> name) {
        Map<UUID, String> names = new LinkedHashMap<>();
        for (T item : items) {
            names.putIfAbsent(id.apply(item), name.apply(item));
        }
        return names;
    }
}
