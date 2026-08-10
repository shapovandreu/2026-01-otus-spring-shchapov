package ru.inversion.wharf.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ControlPlaneClientConfig {

    @Bean
    public WebClient controlPlaneWebClient(AgentProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.gatewayUrl())
                .build();
    }
}
