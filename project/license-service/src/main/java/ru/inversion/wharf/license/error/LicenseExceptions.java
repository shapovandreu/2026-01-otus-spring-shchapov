package ru.inversion.wharf.license.error;

import java.util.UUID;

import ru.inversion.wharf.common.error.DomainException;
import ru.inversion.wharf.common.error.ErrorCode;

public final class LicenseExceptions {

    public static class EntitlementMissing extends DomainException {
        public EntitlementMissing(UUID orgId, UUID productId, String channel) {
            super(ErrorCode.FORBIDDEN,
                    "Нет действующего права: org=" + orgId + ", product=" + productId + ", channel=" + channel);
        }
    }

    public static class EntitlementNotFound extends DomainException {
        public EntitlementNotFound(UUID entitlementId) {
            super(ErrorCode.NOT_FOUND, "Право не найдено: " + entitlementId);
        }
    }

    public static class MissingOrg extends DomainException {
        public MissingOrg() {
            super(ErrorCode.FORBIDDEN, "Токен без организации: операция доступна только субъекту клиента");
        }
    }

    private LicenseExceptions() {
    }
}
