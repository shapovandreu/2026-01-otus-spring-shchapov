package ru.inversion.wharf.console.web;

import ru.inversion.wharf.console.client.ControlPlaneException;
import ru.inversion.wharf.console.security.ConsoleAuthFilter;
import ru.inversion.wharf.console.security.OperatorSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;

@ControllerAdvice
public class ConsoleAdvice {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAdvice.class);

    @ModelAttribute("operator")
    public OperatorSession operator(ServerWebExchange exchange) {
        return exchange.getAttribute(ConsoleAuthFilter.SESSION_ATTRIBUTE);
    }

    @ExceptionHandler(ControlPlaneException.class)
    public Rendering onControlPlane(ControlPlaneException exception) {
        if (exception.isUnauthorized()) {
            return Rendering.redirectTo("/login?expired").build();
        }
        log.debug("Control Plane отказал: {} {}", exception.status(), exception.getMessage());
        return Rendering.view("error")
                .modelAttribute("status", exception.status().value())
                .modelAttribute("code", exception.code())
                .modelAttribute("message", exception.getMessage())
                .status(HttpStatus.OK)
                .build();
    }

    @ExceptionHandler(Forms.FormException.class)
    public Rendering onForm(Forms.FormException exception) {
        return Rendering.view("error")
                .modelAttribute("status", HttpStatus.BAD_REQUEST.value())
                .modelAttribute("code", "FORM_INVALID")
                .modelAttribute("message", exception.getMessage())
                .status(HttpStatus.OK)
                .build();
    }
}
