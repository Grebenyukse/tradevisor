package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.collect.prices.TelegramNotificationService;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.getSignalIdFromQuery;
import static ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient.TELEGRAM_MESSAGE_LINK;

@Service
@RequiredArgsConstructor
public class EventsCmdHandlerImpl implements TgCallbackQueryHandler {

    private final TelegramApiClient telegramApiClient;
    private final SignalsRepository signalsRepository;
    private final TickersRepository tickersRepository;
    private final List<EventCollector> collectors;
    private final TradevisorProperties tradevisorProperties;
    private final TelegramNotificationService telegramService;



    @Override
    public String commandStartsWith() {
        return "events/";
    }

    @Override
    public void handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Integer signalId = getSignalIdFromQuery(callbackData);
        String messageId = "";
        Signals signal = signalsRepository.findSignalBySignalId(signalId).orElseThrow();
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        var infoMessage = collectors.stream()
                .map(l -> l.collect(ticker))
                .flatMap(List::stream)
                .collect(Collectors.joining("\n\n"));
        String linkToChartMessageFromEventsThread = String.format(TELEGRAM_MESSAGE_LINK,
                tradevisorProperties.integration().telegram().supergroup().chatId(),
                messageId);

        infoMessage += "\n сигнал: " + linkToChartMessageFromEventsThread;
        var eventsMessage = telegramApiClient.sendAndGetMessage(TelegramMessageBuilder.sendSimpleMessage(
                tradevisorProperties.integration().telegram().supergroup().chatId(),
                tradevisorProperties.integration().telegram().supergroup().eventsThreadId(),
                infoMessage));
        String linkToEventsMessageFromChartThread = String.format(TELEGRAM_MESSAGE_LINK,
                tradevisorProperties.integration().telegram().supergroup().chatId(),
                eventsMessage.getMessageId());
        String oldMessageWithChart = ""; // need to get somehow
        telegramService.updateMessage(messageId, oldMessageWithChart + "\n инфо: " + linkToEventsMessageFromChartThread);
    }
}
