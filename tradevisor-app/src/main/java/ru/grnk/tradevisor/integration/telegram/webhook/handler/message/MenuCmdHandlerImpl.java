package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendMenu;

@Service
@RequiredArgsConstructor
public class MenuCmdHandlerImpl implements TgMessageHandler {

    private final TelegramApiClient telegramApiClient;

    @Override
    public String commandStartsWith() {
        return "menu";
    }

    @Override
    public void handle(Message message) {
        telegramApiClient.sendMessage(sendMenu(message.getChatId()));
    }
}
