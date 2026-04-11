package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.sendSimpleMessage;

@Service
@RequiredArgsConstructor
public class DeleteSignalsCmdHandlerImpl implements TgMessageHandler {

    private final TelegramApiClient telegramApiClient;
    private final SignalsRepository signalsRepository;

    @Override
    public String command() {
        return "/delete_signals";
    }

    @Override
    public void handle(Message message) {
        signalsRepository.deleteAllSignals();
        telegramApiClient.sendMessage(sendSimpleMessage(message.getChatId(), "все сигналы удалены"));
    }
}
