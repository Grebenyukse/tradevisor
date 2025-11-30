package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
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
        try {
            String callbackData = callbackQuery.getData();
            Integer signalId = getSignalIdFromQuery(callbackData);
            Message originalMessage = callbackQuery.getMessage();
            if (originalMessage == null) {
                throw new IllegalStateException("Original message is null");
            }
            Integer originalMessageId = originalMessage.getMessageId();
            Integer originalMessageThreadId = originalMessage.getMessageThreadId();
            Long chatId = originalMessage.getChatId();
            Signals signal = signalsRepository.findSignalBySignalId(signalId)
                    .orElseThrow(() -> new IllegalArgumentException("Signal not found: " + signalId));
            Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
            var infoMessage = collectors.stream()
                    .map(l -> l.collect(ticker))
                    .flatMap(List::stream)
                    .collect(Collectors.joining("\n\n"));
            String eventsThreadChatId = tradevisorProperties.integration().telegram().supergroup().chatId().toString();
            Integer eventsThreadId = tradevisorProperties.integration().telegram().supergroup().eventsThreadId();
            Message eventsMessage = telegramApiClient.sendAndGetMessage(
                    TelegramMessageBuilder.sendSimpleMessage(
                            Long.parseLong(eventsThreadChatId),
                            eventsThreadId,
                            infoMessage
                    )
            );
            if (eventsMessage == null) {
                throw new IllegalStateException("Failed to send events message");
            }
            Integer eventsMessageId = eventsMessage.getMessageId();
            String linkToEventsMessage = String.format(TELEGRAM_MESSAGE_LINK,
                    eventsThreadChatId.replace("-100", ""), // Убираем префикс для ссылки
                    eventsThreadId,
                    eventsMessageId);

            String linkToOriginalMessage = String.format(TELEGRAM_MESSAGE_LINK,
                    chatId.toString().replace("-100", ""), // Убираем префикс для ссылки
                    originalMessageThreadId,
                    originalMessageId);

            String updatedEventsMessageText = infoMessage + "\n\n<a href=\""+ linkToOriginalMessage + "\">Перейти к сигналу</a>";
            EditMessageText editEventsMessage = new EditMessageText();
            editEventsMessage.setChatId(eventsThreadChatId);
            editEventsMessage.setMessageId(eventsMessageId);
            editEventsMessage.setText(updatedEventsMessageText);
            editEventsMessage.setParseMode(ParseMode.HTML);
            telegramApiClient.editMessageText(editEventsMessage);

            String originalMessageText = originalMessage.getText() != null ? originalMessage.getText() : "";
            String updatedOriginalMessageText = originalMessageText + "\n\n<a href=\"" + linkToEventsMessage + "\">AI отчёт</a>";

            EditMessageText editOriginalMessage = new EditMessageText();
            editOriginalMessage.setChatId(chatId.toString());
            editOriginalMessage.setMessageId(originalMessageId);
            editOriginalMessage.setText(updatedOriginalMessageText);
            editOriginalMessage.setParseMode(ParseMode.HTML);
            telegramApiClient.editMessageText(editOriginalMessage);

        } catch (Exception e) {
            try {
                String errorMessage = "Ошибка при обработке события: " + e.getMessage();
                telegramService.sendMessage(errorMessage,
                        tradevisorProperties.integration().telegram().supergroup().errorsThreadId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
