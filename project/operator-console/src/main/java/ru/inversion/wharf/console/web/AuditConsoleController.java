package ru.inversion.wharf.console.web;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ru.inversion.wharf.common.audit.AuditActions;
import ru.inversion.wharf.console.client.ControlPlaneClient;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.OperatorSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Controller
public class AuditConsoleController {

    private static final int PAGE_SIZE = 50;

    private static final List<String> ACTIONS = List.of(
            AuditActions.CREATE_ORGANIZATION,
            AuditActions.UPDATE_PRODUCT,
            AuditActions.DELETE_PRODUCT,
            AuditActions.PUBLISH_RELEASE,
            AuditActions.UPDATE_RELEASE,
            AuditActions.DELETE_RELEASE,
            AuditActions.GRANT_ENTITLEMENT,
            AuditActions.REVOKE_ENTITLEMENT,
            AuditActions.ISSUE_ENROLLMENT_TOKEN,
            AuditActions.REVOKE_ENROLLMENT_TOKEN);

    private final ControlPlaneClient controlPlane;

    public AuditConsoleController(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    @GetMapping("/audit")
    public Mono<Rendering> entries(@RequestParam(required = false) String actor,
                                   @RequestParam(required = false) UUID org,
                                   @RequestParam(required = false) String action,
                                   @RequestParam(defaultValue = "0") int page,
                                   ServerWebExchange exchange) {
        OperatorSession session = ConsoleAuthFilter.require(exchange);
        return Mono.zip(
                        controlPlane.audit(session.token(), actor, org, action, page, PAGE_SIZE).collectList(),
                        controlPlane.organizations(session.token()).collectList())
                .map(loaded -> Rendering.view("audit")
                        .modelAttribute("entries", loaded.getT1())
                        .modelAttribute("orgs", loaded.getT2())
                        .modelAttribute("orgNames", loaded.getT2().stream()
                                .collect(Collectors.toMap(o -> o.id(), o -> o.name())))
                        .modelAttribute("actor", actor)
                        .modelAttribute("selectedOrg", org)
                        .modelAttribute("action", action)
                        .modelAttribute("actions", ACTIONS)
                        .modelAttribute("page", page)
                        .modelAttribute("baseUrl", baseUrl(actor, org, action))
                        .modelAttribute("hasNext", loaded.getT1().size() == PAGE_SIZE)
                        .build());
    }

    private static String baseUrl(String actor, UUID org, String action) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/audit");
        if (actor != null && !actor.isBlank()) {
            uri.queryParam("actor", actor);
        }
        if (org != null) {
            uri.queryParam("org", org);
        }
        if (action != null && !action.isBlank()) {
            uri.queryParam("action", action);
        }
        return uri.build().toUriString();
    }
}
