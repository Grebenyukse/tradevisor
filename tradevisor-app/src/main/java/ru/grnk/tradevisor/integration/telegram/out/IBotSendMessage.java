package ru.grnk.tradevisor.integration.telegram.out;

import ru.grnk.tradevisor.integration.telegram.BotMessage;

public interface IBotSendMessage {

    void sendMessage(BotMessage message);
}
