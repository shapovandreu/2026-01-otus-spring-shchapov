package ru.inversion.wharf.agent.pull;

import ru.inversion.wharf.agent.cp.EnrollmentService;
import ru.inversion.wharf.agent.cp.OrgAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

    private final EnrollmentService enrollment;
    private final OrgAdminBootstrap orgAdmin;
    private final PullLoop pullLoop;

    public AgentRunner(EnrollmentService enrollment, OrgAdminBootstrap orgAdmin, PullLoop pullLoop) {
        this.enrollment = enrollment;
        this.orgAdmin = orgAdmin;
        this.pullLoop = pullLoop;
    }

    @Override
    public void run(ApplicationArguments args) {
        enrollment.enroll()
                .flatMap(enrolled -> orgAdmin.bootstrap(enrolled.orgName()).thenReturn(true))
                .defaultIfEmpty(false)
                .doOnNext(enrolled -> {
                    if (enrolled) {
                        pullLoop.start();
                    } else {
                        log.info("агент не зарегистрирован — pull-цикл не запускается");
                    }
                })
                .subscribe();
    }
}
