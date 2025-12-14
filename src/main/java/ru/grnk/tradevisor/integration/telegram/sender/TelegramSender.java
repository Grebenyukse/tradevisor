package ru.grnk.tradevisor.integration.telegram.sender;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.buildChartMessage;

@Service
@RequiredArgsConstructor
public class TelegramSender {

    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    public void sendMessage(String url, String title, String text, Integer id, Integer threadId) {
        Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
        telegramApiClient.sendMessage(buildChartMessage(chatId, threadId, url, title, text, id));
    }
}
