package ru.grnk.tradevisor.integration.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import ru.grnk.tradevisor.integration.testconfig.TestConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class TelegramApi {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private TelegramApi() {}

    public static String getCurrentWebhook() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TestConfig.telegramApiBase() + "/getWebhookInfo");
            ClassicHttpResponse response = client.execute(post);
            String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> json = MAPPER.readValue(body, Map.class);
            if (Boolean.TRUE.equals(json.get("ok"))) {
                Map<String, Object> result = (Map<String, Object>) json.get("result");
                return (String) result.getOrDefault("url", "");
            } else {
                throw new RuntimeException("Telegram API error: " + body);
            }
        }
    }

    public static void setWebhook(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TestConfig.telegramApiBase() + "/setWebhook");
            String jsonPayload = MAPPER.writeValueAsString(Map.of("url", url));
            post.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
            ClassicHttpResponse response = client.execute(post);
            String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> jsonResponse = MAPPER.readValue(body, Map.class);
            if (!Boolean.TRUE.equals(jsonResponse.get("ok"))) {
                throw new RuntimeException("Failed to set webhook: " + body);
            }
        }
    }

    public static void deleteWebhook() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(TestConfig.telegramApiBase() + "/deleteWebhook");
            ClassicHttpResponse response = client.execute(post);
            String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> jsonResponse = MAPPER.readValue(body, Map.class);
            if (!Boolean.TRUE.equals(jsonResponse.get("ok"))) {
                throw new RuntimeException("Failed to delete webhook: " + body);
            }
        }
    }
}
