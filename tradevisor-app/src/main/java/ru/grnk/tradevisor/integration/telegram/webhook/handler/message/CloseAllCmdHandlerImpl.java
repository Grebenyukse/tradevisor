package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendSimpleMessage;

@Service
@RequiredArgsConstructor
public class CloseAllCmdHandlerImpl implements TgMessageHandler {

    private final TelegramApiClient telegramApiClient;

    @Override
    public String command() {
        return "close_all";
    }

    @Override
    public void handle(Message message) {
        telegramApiClient.sendMessage(sendSimpleMessage(message.getChatId(), "пользователь запросил закрытие всех открытых позиций"));
    }
}
