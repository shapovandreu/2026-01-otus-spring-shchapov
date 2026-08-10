package ru.inversion.wharf.auth.config;

import java.time.Instant;

import ru.inversion.wharf.auth.domain.OperatorUser;
import ru.inversion.wharf.auth.repository.OperatorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OperatorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperatorSeeder.class);

    private final OperatorUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public OperatorSeeder(OperatorUserRepository users, PasswordEncoder passwordEncoder,
                          AuthProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Flux.fromIterable(properties.seedOperators())
                .concatMap(this::seed)
                .doOnNext(user -> log.info("Создан оператор вендора {} с ролями {}", user.username(), user.roles()))
                .then()
                .block();
    }

    private Mono<OperatorUser> seed(AuthProperties.SeedOperator seed) {
        return users.findByUsername(seed.username())
                .hasElement()
                .filter(exists -> !exists)
                .flatMap(absent -> users.save(OperatorUser.create(
                        seed.username(),
                        passwordEncoder.encode(seed.password()),
                        seed.roles(),
                        Instant.now())));
    }
}
