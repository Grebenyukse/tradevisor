package ru.grnk.tradevisor.eintegration.telegram.out;

import ru.grnk.tradevisor.eintegration.telegram.BotMessage;

public interface IBotSendMessage {

    void sendMessage(BotMessage message);
}
