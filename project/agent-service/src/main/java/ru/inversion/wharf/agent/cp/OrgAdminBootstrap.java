package ru.inversion.wharf.agent.cp;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrgAdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(OrgAdminBootstrap.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PASSWORD_BYTES = 12;
    private static final String LOGIN_SUFFIX = "-admin";
    private static final int MAX_NAME_LENGTH = 100 - LOGIN_SUFFIX.length();

    private final ControlPlaneClient controlPlane;

    public OrgAdminBootstrap(ControlPlaneClient controlPlane) {
        this.controlPlane = controlPlane;
    }

    public Mono<Void> bootstrap(String orgName) {
        String login = loginFor(orgName);
        String password = generatePassword();
        return controlPlane.createOrgAdmin(login, password)
                .doOnNext(created -> announce(login, password))
                .onErrorResume(error -> {
                    log.error("не удалось завести администратора организации {}: {}", login, error.toString());
                    return Mono.empty();
                })
                .then();
    }

    private static void announce(String login, String password) {
        log.info("""

                ┌── учётная запись администратора организации ──────────────────────
                │ консоль клиента: http://localhost:8087
                │ логин:  {}
                │ пароль: {}
                └── пароль показан один раз: он есть только в этом логе ────────────
                """, login, password);
    }

    static String loginFor(String orgName) {
        String slug = orgName == null ? "" : orgName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-яё]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            slug = "org";
        }
        if (slug.length() > MAX_NAME_LENGTH) {
            slug = slug.substring(0, MAX_NAME_LENGTH);
        }
        return slug + LOGIN_SUFFIX;
    }

    private static String generatePassword() {
        byte[] bytes = new byte[PASSWORD_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
