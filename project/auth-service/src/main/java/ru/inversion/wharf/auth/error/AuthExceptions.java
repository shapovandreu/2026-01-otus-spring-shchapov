package ru.inversion.wharf.auth.error;

import ru.inversion.wharf.common.error.DomainException;
import ru.inversion.wharf.common.error.ErrorCode;

public final class AuthExceptions {

    public static class InvalidCredentials extends DomainException {
        public InvalidCredentials() {
            super(ErrorCode.INVALID_CREDENTIALS, "Неверный логин или пароль");
        }
    }

    public static class TokenInvalid extends DomainException {
        public TokenInvalid() {
            super(ErrorCode.TOKEN_INVALID, "Enrollment-токен неизвестен или отозван");
        }
    }

    public static class TokenAlreadyUsed extends DomainException {
        public TokenAlreadyUsed() {
            super(ErrorCode.TOKEN_ALREADY_USED, "Enrollment-токен уже использован");
        }
    }

    public static class TokenExpired extends DomainException {
        public TokenExpired() {
            super(ErrorCode.TOKEN_EXPIRED, "Enrollment-токен истёк");
        }
    }

    public static class OrganizationNotFound extends DomainException {
        public OrganizationNotFound(Object orgId) {
            super(ErrorCode.NOT_FOUND, "Организация не найдена: " + orgId);
        }
    }

    public static class OrganizationExists extends DomainException {
        public OrganizationExists(String name) {
            super(ErrorCode.ALREADY_EXISTS, "Организация уже существует: " + name);
        }
    }

    public static class UserExists extends DomainException {
        public UserExists(String username) {
            super(ErrorCode.ALREADY_EXISTS, "Пользователь уже существует: " + username);
        }
    }

    public static class MissingOrg extends DomainException {
        public MissingOrg() {
            super(ErrorCode.FORBIDDEN, "Токен без организации: операция доступна только субъекту клиента");
        }
    }

    public static class UserNotFound extends DomainException {
        public UserNotFound(Object userId) {
            super(ErrorCode.NOT_FOUND, "Пользователь не найден: " + userId);
        }
    }

    public static class OrganizationInUse extends DomainException {
        public OrganizationInUse(Object orgId) {
            super(ErrorCode.ORGANIZATION_IN_USE,
                    "У организации " + orgId + " есть агенты или enrollment-токены: удаление запрещено");
        }
    }

    private AuthExceptions() {
    }
}
