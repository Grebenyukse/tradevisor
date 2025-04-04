package ru.grnk.tradevisor.zcommon.telegram.out;

import ru.grnk.tradevisor.zcommon.telegram.BotMessage;

public interface IBotSendMessage {

    void sendMessage(BotMessage message);
}
