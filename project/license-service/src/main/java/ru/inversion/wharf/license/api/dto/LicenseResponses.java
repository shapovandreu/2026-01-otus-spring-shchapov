package ru.inversion.wharf.license.api.dto;

import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.license.domain.Entitlement;

public final class LicenseResponses {

    public record EntitlementView(UUID id, UUID orgId, UUID productId, String channel, Instant validUntil) {

        public static EntitlementView of(Entitlement entitlement) {
            return new EntitlementView(entitlement.id(), entitlement.orgId(), entitlement.productId(),
                    entitlement.channel(), entitlement.validUntil());
        }
    }

    private LicenseResponses() {
    }
}
