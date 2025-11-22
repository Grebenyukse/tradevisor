package ru.grnk.tradevisor.integration.telegram.webhook;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiClient {

    private final RestTemplate restTemplate;
    private final TradevisorProperties tradevisorProperties;
    private final ObjectMapper om;

    @PostConstruct
    public void initWebHook() {
        log.info("init webhook");
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

    public String sendDocument(Message originalMessage, String fileContent, String filename, String caption) {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/sendDocument";
            // Создаем multipart тело запроса
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", originalMessage.getChatId().toString());
            body.add("reply_to_message_id", originalMessage.getMessageId());
            body.add("caption", caption);
            body.add("allow_sending_without_reply", true);

            // Создаем Resource для файла
            ByteArrayResource fileResource = new ByteArrayResource(fileContent.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return filename + ".txt";
                }
            };

            body.add("document", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error sending document", e);
            return "error: " + e.getMessage();
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

    public Message sendAndGetMessage(SendMessage sendMessage) {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SendMessage> entity = new HttpEntity<>(sendMessage, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JavaType type = om.getTypeFactory()
                        .constructParametricType(ApiResponse.class, Message.class);
                ApiResponse<Message> apiResponse = om.readValue(response.getBody(), type);
                if (apiResponse != null && apiResponse.getOk()) {
                    return apiResponse.getResult();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error sending message", e);
            return null;
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

    public boolean deleteMessage(DeleteMessage deleteMessage) {
        String token = tradevisorProperties.integration().telegram().chatToken();
        try {
            String apiUrl = "https://api.telegram.org/bot" + token + "/deleteMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DeleteMessage> entity = new HttpEntity<>(deleteMessage, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting message", e);
            return false;
        }
    }
}
