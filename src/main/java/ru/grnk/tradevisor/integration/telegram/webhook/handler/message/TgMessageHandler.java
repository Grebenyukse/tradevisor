package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface TgMessageHandler {

    String command();

    void handle(Message message);

}
