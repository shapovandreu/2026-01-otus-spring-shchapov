package ru.inversion.wharf.auth.service;

import ru.inversion.wharf.auth.error.AuthExceptions;
import ru.inversion.wharf.auth.repository.OperatorUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OperatorAuthService {

    private final OperatorUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public OperatorAuthService(OperatorUserRepository users, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    public Mono<JwtIssuer.IssuedToken> login(String username, String password) {
        return users.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.passwordHash()))
                .map(user -> user.belongsToOrganization()
                        ? jwtIssuer.forOrgUser(user.username(), user.roleList(), user.orgId())
                        : jwtIssuer.forOperator(user.username(), user.roleList()))
                .switchIfEmpty(Mono.error(AuthExceptions.InvalidCredentials::new));
    }
}
