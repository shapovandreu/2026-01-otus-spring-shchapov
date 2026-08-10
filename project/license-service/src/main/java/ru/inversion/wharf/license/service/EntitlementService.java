package ru.inversion.wharf.license.service;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.license.domain.Entitlement;
import ru.inversion.wharf.license.error.LicenseExceptions;
import ru.inversion.wharf.license.repository.EntitlementRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EntitlementService {

    private final EntitlementRepository entitlements;

    public EntitlementService(EntitlementRepository entitlements) {
        this.entitlements = entitlements;
    }

    public Mono<Entitlement> grant(UUID orgId, UUID productId, String channel, Instant validUntil) {
        return entitlements.findByOrgIdAndProductIdAndChannel(orgId, productId, channel)
                .map(existing -> existing.validUntil(validUntil))
                .defaultIfEmpty(Entitlement.grant(orgId, productId, channel, validUntil, Instant.now()))
                .flatMap(entitlements::save);
    }

    public Mono<Entitlement> changeValidUntil(UUID entitlementId, Instant validUntil) {
        return byId(entitlementId)
                .map(entitlement -> entitlement.validUntil(validUntil))
                .flatMap(entitlements::save);
    }

    public Mono<Entitlement> revoke(UUID entitlementId) {
        return byId(entitlementId)
                .flatMap(entitlement -> entitlements.delete(entitlement).thenReturn(entitlement));
    }

    public Flux<Entitlement> list(UUID orgId) {
        return orgId == null ? entitlements.findAll() : entitlements.findByOrgId(orgId);
    }

    public Mono<Entitlement> byId(UUID entitlementId) {
        return entitlements.findById(entitlementId)
                .switchIfEmpty(Mono.error(() -> new LicenseExceptions.EntitlementNotFound(entitlementId)));
    }

    public Mono<Entitlement> require(UUID orgId, UUID productId, String channel) {
        return entitlements.findByOrgIdAndProductIdAndChannel(orgId, productId, channel)
                .filter(entitlement -> entitlement.isValid(Instant.now()))
                .switchIfEmpty(Mono.error(() -> new LicenseExceptions.EntitlementMissing(orgId, productId, channel)));
    }

    public Flux<Entitlement> listValid(UUID orgId) {
        Instant now = Instant.now();
        return entitlements.findByOrgId(orgId).filter(entitlement -> entitlement.isValid(now));
    }

    public Mono<Boolean> isProductInUse(UUID productId) {
        return entitlements.countByProductId(productId).map(count -> count > 0);
    }

    public Mono<Boolean> isAllowed(UUID orgId, UUID productId, String channel) {
        return entitlements.findByOrgIdAndProductIdAndChannel(orgId, productId, channel)
                .map(entitlement -> entitlement.isValid(Instant.now()))
                .defaultIfEmpty(false);
    }
}
