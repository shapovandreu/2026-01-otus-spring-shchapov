package ru.inversion.wharf.client.web;

import ru.inversion.wharf.client.client.ControlPlaneClient;
import ru.inversion.wharf.client.client.ControlPlaneException;
import ru.inversion.wharf.client.security.ClientSession;
import ru.inversion.wharf.client.security.SessionCookies;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class LoginController {

    private final ControlPlaneClient controlPlane;
    private final SessionCookies cookies;

    public LoginController(ControlPlaneClient controlPlane, SessionCookies cookies) {
        this.controlPlane = controlPlane;
        this.cookies = cookies;
    }

    @GetMapping("/login")
    public Rendering form(@RequestParam(required = false) String expired) {
        return Rendering.view("login")
                .modelAttribute("expired", expired != null)
                .build();
    }

    @PostMapping("/login")
    public Mono<Rendering> submit(ServerWebExchange exchange) {
        return exchange.getFormData()
                .map(Forms::of)
                .flatMap(form -> controlPlane.login(form.text("username"), form.text("password")))
                .map(token -> ClientSession.parse(token.accessToken())
                        .filter(ClientSession::isOrgAdmin)
                        .map(session -> {
                            cookies.write(exchange, token.accessToken(), token.expiresAt());
                            return Rendering.redirectTo("/").build();
                        })
                        .orElseGet(() -> Rendering.view("login")
                                .modelAttribute("error", "Эта консоль — для администратора организации. "
                                        + "Операторы вендора работают в операторской консоли.")
                                .build()))
                .onErrorResume(ControlPlaneException.class, error -> Mono.just(Rendering.view("login")
                        .modelAttribute("error", error.isUnauthorized()
                                ? "Неверный логин или пароль"
                                : error.getMessage())
                        .build()));
    }

    @PostMapping("/logout")
    public Rendering logout(ServerWebExchange exchange) {
        cookies.clear(exchange);
        return Rendering.redirectTo("/login").build();
    }
}
