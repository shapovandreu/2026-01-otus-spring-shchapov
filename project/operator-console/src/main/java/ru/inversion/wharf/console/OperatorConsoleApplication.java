package ru.inversion.wharf.console;

import ru.inversion.wharf.console.config.ConsoleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConsoleProperties.class)
public class OperatorConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperatorConsoleApplication.class, args);
    }
}
