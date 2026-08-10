package ru.inversion.wharf.catalog;

import ru.inversion.wharf.common.audit.AuditConfig;
import ru.inversion.wharf.common.error.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ApiExceptionHandler.class, AuditConfig.class})
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
