package ru.inversion.wharf.client;

import ru.inversion.wharf.client.config.ClientConsoleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientConsoleProperties.class)
public class ClientConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientConsoleApplication.class, args);
    }
}
