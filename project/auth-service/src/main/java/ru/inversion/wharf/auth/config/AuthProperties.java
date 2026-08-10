package ru.inversion.wharf.auth.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iw.auth")
public record AuthProperties(Jwt jwt, Enrollment enrollment, List<SeedOperator> seedOperators) {

    public record Jwt(String issuer, Duration operatorTtl, Duration agentTtl) {
    }

    public record Enrollment(Duration defaultTtl) {
    }

    public record SeedOperator(String username, String password, String roles) {
    }

    public List<SeedOperator> seedOperators() {
        return seedOperators == null ? List.of() : seedOperators;
    }
}
