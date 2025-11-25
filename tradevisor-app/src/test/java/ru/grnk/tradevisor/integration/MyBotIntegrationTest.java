package ru.grnk.tradevisor.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvTelegramProperties;
import ru.grnk.tradevisor.integration.extension.TelegramWebhookExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TelegramWebhookExtension.class)
class MyBotIntegrationTest {

    @Autowired
    private TradevisorProperties tradevisorProperties;

    @Test
    void testWebhookIsAccessible() {
        TrvTelegramProperties telegramProperties = tradevisorProperties.integration().telegram();
        String webhookUrl = String.format("https://api.telegram.org/bot%s:%s", telegramProperties.chatId(), telegramProperties.baseUrl());
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(
                webhookUrl + "/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
