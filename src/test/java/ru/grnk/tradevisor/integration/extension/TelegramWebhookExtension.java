package ru.grnk.tradevisor.integration.extension;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import ru.grnk.tradevisor.integration.testconfig.TestConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static ru.grnk.tradevisor.integration.extension.TelegramApi.*;

public class TelegramWebhookExtension implements BeforeAllCallback, AfterAllCallback, ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookExtension.class);

    private GenericContainer<?> cloContainer;
    private String publicUrl;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        startCloContainer();
        waitForPublicUrl();
        registerWebhookIfNecessary();
    }

    private void startCloContainer() {
        String image = TestConfig.cloImage();
        int httpPort = TestConfig.cloHttpPort();
        int botPort = TestConfig.botPort();

        cloContainer = new GenericContainer<>(DockerImageName.parse(image))
            .withExposedPorts(httpPort, botPort)
            .withEnv("CLO_TUNNEL", String.valueOf(botPort))
            .withEnv("CLO_TUNNEL_HOST", TestConfig.botHost())
            .withEnv("CLO_TUNNEL_PROTO", "http")
            .withEnv("CLO_TUNNEL_TLS", "true") // Important: Telegram requires HTTPS
            .waitingFor(Wait.forHttp("/api/tunnels").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(30)));

        cloContainer.start();
    }

    private void waitForPublicUrl() throws IOException, InterruptedException {
        int httpPortOnHost = cloContainer.getMappedPort(TestConfig.cloHttpPort());
        String apiUrl = "http://" + cloContainer.getHost() + ":" + httpPortOnHost + "/api/tunnels";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                HttpGet request = new HttpGet(apiUrl);
                var response = client.execute(request);
                if (response.getCode() == 200) {
                    String json = new String(response.getEntity().getContent().readAllBytes());
                    Map<String, Object> map = MAPPER.readValue(json, Map.class);
                    var tunnels = (Iterable<?>) map.get("tunnels");
                    if (tunnels != null && tunnels.iterator().hasNext()) {
                        Map<String, Object> firstTunnel = (Map<String, Object>) tunnels.iterator().next();
                        publicUrl = ((String) firstTunnel.get("public_url")).replaceAll("/$", "");
                        log.info("CLO tunnel is ready: {}", publicUrl);
                        return;
                    }
                }
                Thread.sleep(500);
            }
            throw new IllegalStateException("Timed out waiting for CLO tunnel");
        }
    }

    private void registerWebhookIfNecessary() throws IOException {
        String current = getCurrentWebhook();
        if (!current.equals(publicUrl)) {
            log.info("Updating Telegram webhook from '{}' to '{}'", current, publicUrl);
            setWebhook(publicUrl);
        } else {
            log.info("Telegram webhook already set to {}", publicUrl);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try {
            deleteWebhook();
        } catch (Exception e) {
            log.warn("Failed to delete Telegram webhook", e);
        } finally {
            if (cloContainer != null) {
                cloContainer.stop();
            }
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (publicUrl != null) {
            TestPropertyValues.of("telegram.webhook.url=" + publicUrl)
                .applyTo(applicationContext.getEnvironment());
        }
    }
}
