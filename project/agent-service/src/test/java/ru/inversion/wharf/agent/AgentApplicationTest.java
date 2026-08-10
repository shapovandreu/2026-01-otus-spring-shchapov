package ru.inversion.wharf.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "iw.agent.enrollment-token=",
        "iw.agent.gateway-url=http://localhost:8080",
})
class AgentApplicationTest {

    @Test
    void contextLoads() {
    }
}
