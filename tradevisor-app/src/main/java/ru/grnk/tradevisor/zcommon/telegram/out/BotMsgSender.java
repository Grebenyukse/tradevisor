package ru.grnk.tradevisor.zcommon.telegram.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.grnk.tradevisor.zcommon.telegram.BotMessage;

@Service
public class BotMsgSender implements IBotSendMessage {

    @Value("${telegram.chat-id}")
    private String chatId;

    @Value("${telegram.chat-token}")
    private String chatToken;

    @Override
    public void sendMessage(BotMessage message) {
        SendMessage sendMessage = SendMessage.builder()
                .allowSendingWithoutReply(true)
                .chatId(chatId)
                .text(message.title() + message.text())
                .build();
    }
}
