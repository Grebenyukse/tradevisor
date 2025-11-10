package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.*;

@Service
@RequiredArgsConstructor
public class DeleteCmdHandlerImpl implements TgCallbackQueryHandler {

    private final SignalsRepository signalsRepository;
    private final TelegramApiClient telegramApiClient;
    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;

    @Override
    public String commandStartsWith() {
        return "delete/";
    }

    @Override
    public void handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        Integer signalId = getSignalIdFromQuery(callbackData);
        Signals signal = signalsRepository.findSignalBySignalId(signalId).orElseThrow();
        tickersRepository.markTickerFailedByUser(signal.getTickerCode());
        marketDataRepository.deleteMarketData(signal.getTickerCode());
        signalsRepository.updateSignalStatus(signalId, TrvSignalStatus.CANCELLED);
        telegramApiClient.deleteMessage(buildDeleteMessage(message));
    }
}
