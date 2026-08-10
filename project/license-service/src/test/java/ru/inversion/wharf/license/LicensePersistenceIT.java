package ru.inversion.wharf.license;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import ru.inversion.wharf.license.domain.Entitlement;
import ru.inversion.wharf.license.service.EntitlementService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
class LicensePersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private EntitlementService entitlements;

    @Test
    void expiredEntitlementIsNotAllowedOnRealDatabase() {
        UUID org = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        entitlements.grant(org, product, "stable", Instant.now().minus(Duration.ofDays(1))).block();

        StepVerifier.create(entitlements.isAllowed(org, product, "stable"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void grantingTwiceUpdatesTheSameEntitlementOnRealDatabase() {
        UUID org = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        Instant first = Instant.now().plus(Duration.ofDays(10));
        Instant second = Instant.now().plus(Duration.ofDays(90));

        Entitlement granted = entitlements.grant(org, product, "stable", first).block();
        assertThat(granted).isNotNull();

        Entitlement regranted = entitlements.grant(org, product, "stable", second).block();
        assertThat(regranted).isNotNull();
        assertThat(regranted.id()).isEqualTo(granted.id());
        assertThat(regranted.validUntil()).isEqualTo(second);

        StepVerifier.create(entitlements.list(org))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void revokingEntitlementClosesChannelOnRealDatabase() {
        UUID org = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        Entitlement granted = entitlements.grant(org, product, "beta", null).block();
        assertThat(granted).isNotNull();

        entitlements.revoke(granted.id()).block();

        StepVerifier.create(entitlements.isAllowed(org, product, "beta"))
                .expectNext(false)
                .verifyComplete();
    }
}
