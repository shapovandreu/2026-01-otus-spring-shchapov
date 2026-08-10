package ru.inversion.wharf.auth.api;

import java.util.Map;

import ru.inversion.wharf.auth.api.dto.EnrollRequest;
import ru.inversion.wharf.auth.api.dto.EnrollResponse;
import ru.inversion.wharf.auth.api.dto.LoginRequest;
import ru.inversion.wharf.auth.api.dto.TokenResponse;
import ru.inversion.wharf.auth.service.EnrollmentService;
import ru.inversion.wharf.auth.service.OperatorAuthService;
import ru.inversion.wharf.common.api.ApiVersion;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/auth")
public class AuthController {

    private final OperatorAuthService operatorAuth;
    private final EnrollmentService enrollment;
    private final RSAKey rsaKey;

    public AuthController(OperatorAuthService operatorAuth, EnrollmentService enrollment, RSAKey rsaKey) {
        this.operatorAuth = operatorAuth;
        this.enrollment = enrollment;
        this.rsaKey = rsaKey;
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return operatorAuth.login(request.username(), request.password())
                .map(TokenResponse::of);
    }

    @PostMapping("/enroll")
    public Mono<EnrollResponse> enroll(@Valid @RequestBody EnrollRequest request) {
        return enrollment.enroll(request.token())
                .map(EnrollResponse::of);
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
