package ru.inversion.wharf.license;

import ru.inversion.wharf.common.audit.AuditConfig;
import ru.inversion.wharf.common.error.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ApiExceptionHandler.class, AuditConfig.class})
public class LicenseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseServiceApplication.class, args);
    }
}
