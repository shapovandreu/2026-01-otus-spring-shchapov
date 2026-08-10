package ru.inversion.wharf.telemetry.api;

import java.util.UUID;

import ru.inversion.wharf.common.api.ApiVersion;
import ru.inversion.wharf.telemetry.domain.AuditDocument;
import ru.inversion.wharf.telemetry.repository.AuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/telemetry/audit")
public class AuditQueryController {

    private final AuditRepository audit;

    public AuditQueryController(AuditRepository audit) {
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RM', 'LM', 'ADMIN')")
    public Flux<AuditDocument> entries(@RequestParam(required = false) String actor,
                                       @RequestParam(required = false) UUID org,
                                       @RequestParam(required = false) String action,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "100") int limit) {
        PageRequest pageRequest = QueryPage.of(page, limit);
        if (actor != null) {
            return audit.findByActorOrderByOccurredAtDesc(actor, pageRequest);
        }
        if (org != null) {
            return audit.findByOrgIdOrderByOccurredAtDesc(org, pageRequest);
        }
        if (action != null) {
            return audit.findByActionOrderByOccurredAtDesc(action, pageRequest);
        }
        return audit.findAllByOrderByOccurredAtDesc(pageRequest);
    }
}
