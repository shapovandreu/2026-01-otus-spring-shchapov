package ru.inversion.wharf.catalog.error;

import java.util.UUID;

import ru.inversion.wharf.common.error.DomainException;
import ru.inversion.wharf.common.error.ErrorCode;

public final class CatalogExceptions {

    public static class ProductNotFound extends DomainException {
        public ProductNotFound(UUID productId) {
            super(ErrorCode.NOT_FOUND, "Продукт не найден: " + productId);
        }
    }

    public static class ReleaseNotFound extends DomainException {
        public ReleaseNotFound(UUID releaseId) {
            super(ErrorCode.NOT_FOUND, "Релиз не найден: " + releaseId);
        }
    }

    public static class ProductInUse extends DomainException {
        public ProductInUse(UUID productId, String what) {
            super(ErrorCode.PRODUCT_IN_USE,
                    "Продукт " + productId + " удалить нельзя: на него ссылаются " + what);
        }
    }

    public static class ReleaseAlreadyPublished extends DomainException {
        public ReleaseAlreadyPublished(UUID releaseId) {
            super(ErrorCode.RELEASE_PUBLISHED,
                    "Релиз " + releaseId + " опубликован: править и удалять можно только черновик");
        }
    }

    public static class ReleaseNotPublished extends DomainException {
        public ReleaseNotPublished(UUID releaseId) {
            super(ErrorCode.NOT_FOUND, "Релиз " + releaseId + " ещё не опубликован");
        }
    }

    public static class NotAnAgent extends DomainException {
        public NotAnAgent() {
            super(ErrorCode.FORBIDDEN, "Токен без организации: агентское API доступно только агенту");
        }
    }

    public static class MissingOrg extends DomainException {
        public MissingOrg() {
            super(ErrorCode.FORBIDDEN, "Токен без организации: операция доступна только субъекту клиента");
        }
    }

    public static class TargetReleaseRequired extends DomainException {
        public TargetReleaseRequired(String action) {
            super(ErrorCode.VALIDATION_FAILED, "Для действия " + action + " нужен целевой релиз");
        }
    }

    public static class ReleaseProductMismatch extends DomainException {
        public ReleaseProductMismatch(UUID releaseId, UUID productId) {
            super(ErrorCode.VALIDATION_FAILED,
                    "Релиз " + releaseId + " не принадлежит продукту " + productId);
        }
    }

    public static class IntentNotFound extends DomainException {
        public IntentNotFound(UUID intentId) {
            super(ErrorCode.NOT_FOUND, "Намерение не найдено: " + intentId);
        }
    }

    public static class EntitlementMissing extends DomainException {
        public EntitlementMissing(UUID productId, String channel) {
            super(ErrorCode.FORBIDDEN,
                    "Нет права на продукт " + productId + " в канале " + channel);
        }
    }

    private CatalogExceptions() {
    }
}
