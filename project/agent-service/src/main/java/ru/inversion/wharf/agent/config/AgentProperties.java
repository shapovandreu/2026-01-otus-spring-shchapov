package ru.inversion.wharf.agent.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iw.agent")
public record AgentProperties(
        String gatewayUrl,
        String enrollmentToken,
        Pull pull) {

    public record Pull(Duration interval, Duration jitter) {
    }
}
