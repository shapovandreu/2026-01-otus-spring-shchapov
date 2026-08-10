package ru.inversion.wharf.common.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.inversion.wharf.common.api.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventTest {

    @Test
    void takesActorAndRolesFromOperatorToken() {
        UUID org = UUID.randomUUID();
        Jwt operator = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("lmanager")
                .claim(Roles.CLAIM_ROLES, List.of("LM"))
                .issuedAt(Instant.parse("2026-07-20T09:00:00Z"))
                .expiresAt(Instant.parse("2026-07-20T10:00:00Z"))
                .build();

        AuditEvent event = AuditEvent.of(operator, AuditActions.GRANT_ENTITLEMENT, "entitlement", "ent-1", org);

        assertThat(event.actor()).isEqualTo("lmanager");
        assertThat(event.roles()).containsExactly("LM");
        assertThat(event.action()).isEqualTo(AuditActions.GRANT_ENTITLEMENT);
        assertThat(event.targetType()).isEqualTo("entitlement");
        assertThat(event.targetId()).isEqualTo("ent-1");
        assertThat(event.orgId()).isEqualTo(org);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void toleratesTokenWithoutRolesClaim() {
        Jwt operator = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("admin")
                .claim("scope", "read")
                .build();

        AuditEvent event = AuditEvent.of(operator, AuditActions.PUBLISH_RELEASE, "release", "rel-1", null);

        assertThat(event.actor()).isEqualTo("admin");
        assertThat(event.roles()).isNull();
        assertThat(event.orgId()).isNull();
    }
}
