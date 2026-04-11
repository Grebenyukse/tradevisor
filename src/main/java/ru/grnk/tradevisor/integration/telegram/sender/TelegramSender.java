package ru.grnk.tradevisor.integration.telegram.sender;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;
import ru.grnk.tradevisor.notify.publish.NotificationService;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.buildChartMessage;

@Service
@RequiredArgsConstructor
public class TelegramSender implements NotificationService {

    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    @Override
    public void sendMessage(String url, String title, String text, Integer id, Integer threadId) {
        Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
        telegramApiClient.sendMessage(buildChartMessage(chatId, threadId, url, title, text, id));
    }
}
