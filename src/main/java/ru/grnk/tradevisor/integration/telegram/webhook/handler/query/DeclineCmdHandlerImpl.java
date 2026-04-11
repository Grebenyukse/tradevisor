package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.*;

@Service
@RequiredArgsConstructor
public class DeclineCmdHandlerImpl implements TgCallbackQueryHandler {

    private final SignalsRepository signalsRepository;
    private final TelegramApiClient telegramApiClient;

    @Override
    public String commandStartsWith() {
        return "decline/";
    }

    @Override
    public void handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        signalsRepository.updateSignalStatus(getSignalIdFromQuery(callbackData), TrvSignalStatus.CANCELLED);
//        telegramApiClient.editMessageText(buildReactionMessage(message, "❌"));
        // удаляем чтобы не захломлять чат с ботом
        telegramApiClient.deleteMessage(buildDeleteMessage(message));
    }
}
