package ru.inversion.wharf.agent.cp;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inversion.wharf.agent.cp.ControlPlaneMessages.Intent;
import ru.inversion.wharf.agent.fsm.Installation;
import ru.inversion.wharf.agent.fsm.InstallationEngine;
import ru.inversion.wharf.agent.fsm.InstallationState;
import ru.inversion.wharf.agent.fsm.InstallationStore;
import ru.inversion.wharf.agent.fsm.IntentCommand;
import ru.inversion.wharf.agent.fsm.SimulatedArtifactPuller;
import ru.inversion.wharf.agent.pull.IntentDispatcher;
import ru.inversion.wharf.agent.telemetry.TelemetryReporter;
import ru.inversion.wharf.agent.verify.ManifestDoc;
import ru.inversion.wharf.agent.verify.TrustAnchors;
import ru.inversion.wharf.agent.verify.VerifyGate;
import ru.inversion.wharf.common.signing.JwsSigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneContractTest {

    private static final UUID RELEASE = UUID.randomUUID();
    private static final UUID INSTALLATION = UUID.randomUUID();
    private static final UUID PRODUCT = UUID.randomUUID();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final JwsSigner catalogSigner = new JwsSigner();
    private final AtomicInteger telemetryPosts = new AtomicInteger();

    private final AtomicInteger consumedIntents = new AtomicInteger();

    private MockWebServer controlPlane;
    private InstallationEngine engine;
    private IntentDispatcher dispatcher;

    @BeforeEach
    void startControlPlane() throws Exception {
        controlPlane = new MockWebServer();
        controlPlane.setDispatcher(dispatcher());
        controlPlane.start();

        WebClient web = WebClient.builder().baseUrl(controlPlane.url("/").toString()).build();
        AgentIdentity identity = new AgentIdentity();
        identity.assign(new AgentIdentity.Token("test-agent-jwt", Instant.now().plusSeconds(3600)));

        ControlPlaneResilience resilience = new ControlPlaneResilience(
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build()));
        ControlPlaneClient client = new ControlPlaneClient(web, identity, resilience);

        engine = new InstallationEngine(client, new TrustAnchors(client), new VerifyGate(mapper),
                new SimulatedArtifactPuller(), new InstallationStore(), new TelemetryReporter(client));
        dispatcher = new IntentDispatcher(engine, client, new TelemetryReporter(client));
    }

    @AfterEach
    void stopControlPlane() throws Exception {
        controlPlane.shutdown();
    }

    @Test
    void agentVerifiesSignedManifestOverHttpAndReportsTelemetry() {
        IntentCommand command = new IntentCommand(UUID.randomUUID(), IntentCommand.Action.INSTALL,
                INSTALLATION, PRODUCT, RELEASE);

        Installation result = engine.apply(command).block();

        assertThat(result).isNotNull();
        assertThat(result.state()).isEqualTo(InstallationState.RUNNING);
        assertThat(result.version()).isEqualTo("1.4.2");
        assertThat(telemetryPosts.get()).isPositive();
    }

    @Test
    void agentRemovesExecutedIntentFromQueue() {
        UUID intentId = UUID.randomUUID();
        Intent intent = new Intent(intentId, INSTALLATION, PRODUCT, "INSTALL", RELEASE,
                "PENDING", Instant.parse("2026-07-20T10:00:00Z"));

        dispatcher.dispatch(intent).block();

        assertThat(consumedIntents.get()).isOne();
    }

    private Dispatcher dispatcher() {
        String jwks = JSONObjectUtils.toJSONString(catalogSigner.jwks());
        ManifestDoc manifest = new ManifestDoc(RELEASE, PRODUCT, "app", "1.4.2", "stable",
                Instant.parse("2026-07-20T10:00:00Z"));
        String manifestJws = catalogSigner.sign(manifest);

        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.equals("/api/v1/catalog/.well-known/jwks.json")) {
                    return json(jwks);
                }
                if (path.startsWith("/api/v1/releases/") && path.endsWith("/manifest")) {
                    return json("{\"jws\":\"" + manifestJws + "\"}");
                }
                if (path.equals("/api/v1/telemetry")) {
                    telemetryPosts.incrementAndGet();
                    return new MockResponse().setResponseCode(202);
                }
                if (path.startsWith("/api/v1/intents/") && path.endsWith("/consume")) {
                    consumedIntents.incrementAndGet();
                    return json("{\"id\":\"" + path.split("/")[4] + "\",\"status\":\"CONSUMED\"}");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
