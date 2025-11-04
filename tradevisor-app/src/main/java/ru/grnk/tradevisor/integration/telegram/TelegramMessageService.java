package ru.grnk.tradevisor.integration.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramBotService;

@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramBotService telegramBotService;
    private final TradevisorProperties tradevisorProperties;

    public void sendMessage(String url, String title, String text) {
        Long chatId = Long.parseLong(tradevisorProperties.integration().telegram().chatId());
        telegramBotService.sendMessage(chatId, url, title, text);
    }

}
