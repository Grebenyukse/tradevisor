package ru.grnk.tradevisor.integration.telegram.webhook.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.grnk.tradevisor.integration.telegram.webhook.handler.message.TgMessageHandler;
import ru.grnk.tradevisor.integration.telegram.webhook.handler.message.WelcomeHandler;
import ru.grnk.tradevisor.integration.telegram.webhook.handler.query.TgCallbackQueryHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class TelegramBotConsumer {

    private final Map<String, TgMessageHandler> messageHandlersMap;
    private final List<TgCallbackQueryHandler> callbackQueryHandlers;
    private final WelcomeHandler welcomeHandler;

    @Autowired
    public TelegramBotConsumer(
            List<TgMessageHandler> messageHandlers,
            List<TgCallbackQueryHandler> callbackQueryHandlers,
            WelcomeHandler welcomeHandler
    ) {
        this.messageHandlersMap = messageHandlers.stream().collect(toMap(TgMessageHandler::commandStartsWith, Function.identity()));
        this.callbackQueryHandlers = callbackQueryHandlers;
        this.welcomeHandler = welcomeHandler;
    }

    public void processUpdate(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                ofNullable(messageHandlersMap.get(update.getMessage().getText()))
                        .ifPresentOrElse(
                                h -> h.handle(update.getMessage()),
                                () -> welcomeHandler.handle(update.getMessage())
                        );
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandlers.stream()
                        .filter(x -> update.getCallbackQuery().getData().startsWith(x.commandStartsWith()))
                                .findFirst()
                                        .ifPresentOrElse(h -> h.handle(update.getCallbackQuery()),
                                                () -> {
                                            throw new RuntimeException("unkown callback query " + update.getCallbackQuery().toString());
                                        });
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

}
