package ru.grnk.tradevisor.telegram.out;

import ru.grnk.tradevisor.telegram.BotMessage;

public interface IBotSendMessage {

    void sendMessage(BotMessage message);
}
