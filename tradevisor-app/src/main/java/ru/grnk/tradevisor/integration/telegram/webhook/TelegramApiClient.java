package ru.grnk.tradevisor.integration.telegram.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import javax.annotation.PostConstruct;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiClient {

    private final RestTemplate restTemplate;
    private final TradevisorProperties tradevisorProperties;

    @PostConstruct
    public void initWebHook() {
        setWebhookIfNeeded();
    }

    public String setWebhook() {
        String callbackUrl = tradevisorProperties.integration().telegram().baseUrl();
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/setWebhook";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = "{\"url\":\"" + callbackUrl + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error setting webhook", e);
            return "Error: " + e.getMessage();
        }
    }

    public String setWebhookIfNeeded() {
        String currentWebhookInfo = getWebhookInfo();
        if (currentWebhookInfo.startsWith("Error:")) {
            return "Cannot check current webhook: " + currentWebhookInfo;
        }
        String expectedUrl = tradevisorProperties.integration().telegram().baseUrl();
        if (currentWebhookInfo.contains("\"url\":\"" + expectedUrl + "\"") &&
                currentWebhookInfo.contains("\"pending_update_count\":0")) {
            return "Webhook is already set correctly";
        }
        if (tradevisorProperties.integration().telegram().reRegister()) {
            deleteWebhook();
        }
        return setWebhook();
    }

    public String getWebhookInfo() {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/getWebhookInfo";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving webhook info", e);
            return "Error: " + e.getMessage();
        }
    }

    public boolean deleteWebhook() {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/deleteWebhook";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting webhook", e);
            return false;
        }
    }

    public boolean sendMessage(SendMessage sendMessage) {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SendMessage> entity = new HttpEntity<>(sendMessage, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return false;
        }
    }

    public boolean editMessageText(EditMessageText editMessageText) {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/editMessageText";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EditMessageText> entity = new HttpEntity<>(editMessageText, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error editing message", e);
            return false;
        }
    }
}
