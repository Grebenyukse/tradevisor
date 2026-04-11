package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.sendMenu;

@Service
@RequiredArgsConstructor
public class StartCmdHandlerImpl implements TgMessageHandler {

    private final TelegramApiClient telegramApiClient;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(Message message) {
        telegramApiClient.sendMessage(sendMenu(message.getChatId()));
    }
}
