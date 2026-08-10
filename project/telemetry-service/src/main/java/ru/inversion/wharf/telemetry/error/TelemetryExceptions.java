package ru.inversion.wharf.telemetry.error;

import ru.inversion.wharf.common.error.DomainException;
import ru.inversion.wharf.common.error.ErrorCode;

public final class TelemetryExceptions {

    public static class NotAnAgent extends DomainException {
        public NotAnAgent() {
            super(ErrorCode.FORBIDDEN, "Токен без организации: телеметрию принимаем только от агента");
        }
    }

    public static class QueryFilterRequired extends DomainException {
        public QueryFilterRequired() {
            super(ErrorCode.VALIDATION_FAILED, "Укажите фильтр: installation или org");
        }
    }

    private TelemetryExceptions() {
    }
}
