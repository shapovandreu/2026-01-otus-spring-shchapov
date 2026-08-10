package ru.inversion.wharf.catalog.config;

import java.time.Duration;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ru.inversion.wharf.catalog.service.ManifestService;
import ru.inversion.wharf.common.signing.JwsSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CatalogConfig {

    @Bean
    public JwsSigner manifestSigner() {
        return new JwsSigner();
    }

    @Bean
    public Cache<UUID, ManifestService.SignedManifest> manifestCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient licenseClient(WebClient.Builder builder,
                                   @Value("${iw.catalog.license-service-url}") String licenseServiceUrl) {
        return builder.baseUrl(licenseServiceUrl).build();
    }
}
