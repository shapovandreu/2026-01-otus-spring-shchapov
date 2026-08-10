package ru.inversion.wharf.console.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableProperties.class);

    @Test
    void bindsCompleteConfiguration() {
        runner.withPropertyValues(
                        "iw.console.gateway-url=http://api-gateway:8080",
                        "iw.console.request-timeout=5s",
                        "iw.console.session.cookie-name=iw_operator",
                        "iw.console.session.secure=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ConsoleProperties properties = context.getBean(ConsoleProperties.class);
                    assertThat(properties.gatewayUrl()).isEqualTo("http://api-gateway:8080");
                    assertThat(properties.session().cookieName()).isEqualTo("iw_operator");
                });
    }

    @Test
    void failsWhenConfigurationIsMissingEntirely() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWithoutGatewayUrl() {
        runner.withPropertyValues(
                        "iw.console.request-timeout=5s",
                        "iw.console.session.cookie-name=iw_operator")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWithoutSessionCookieName() {
        runner.withPropertyValues(
                        "iw.console.gateway-url=http://api-gateway:8080",
                        "iw.console.request-timeout=5s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConsoleProperties.class)
    static class EnableProperties {
    }
}
