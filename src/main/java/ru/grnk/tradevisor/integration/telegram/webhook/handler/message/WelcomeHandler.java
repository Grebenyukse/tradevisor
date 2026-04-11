package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.sendWelcomeMessage;

@Service
@RequiredArgsConstructor
public class WelcomeHandler {

    private final TelegramApiClient telegramApiClient;

    public void handle(Message message) {
        telegramApiClient.sendMessage(sendWelcomeMessage(message.getChatId()));
    }
}
