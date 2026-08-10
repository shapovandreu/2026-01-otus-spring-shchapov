package ru.inversion.wharf.agent.cp;

import ru.inversion.wharf.agent.config.AgentProperties;
import ru.inversion.wharf.agent.cp.AgentIdentity.Token;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.AgentToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final ControlPlaneClient controlPlane;
    private final AgentIdentity identity;
    private final AgentProperties properties;

    public EnrollmentService(ControlPlaneClient controlPlane, AgentIdentity identity,
                             AgentProperties properties) {
        this.controlPlane = controlPlane;
        this.identity = identity;
        this.properties = properties;
    }

    public Mono<AgentToken> enroll() {
        String token = properties.enrollmentToken();
        if (token == null || token.isBlank()) {
            log.info("enrollment-токен не задан — агент стартует вхолостую, pull-цикл не запускается");
            return Mono.empty();
        }
        return controlPlane.enroll(token)
                .doOnNext(agentToken -> {
                    identity.assign(new Token(agentToken.accessToken(), agentToken.expiresAt()));
                    log.info("агент зарегистрирован в организации {} ({}), JWT действует до {}",
                            agentToken.orgName(), agentToken.orgId(), agentToken.expiresAt());
                })
                .onErrorResume(error -> {
                    log.error("enrollment не удался: {}", error.toString());
                    return Mono.empty();
                });
    }
}
