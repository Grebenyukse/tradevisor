package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface TgCallbackQueryHandler {

    String commandStartsWith();

    void handle(CallbackQuery query);
}
