package ru.grnk.tradevisor.integration.telegram.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.grnk.tradevisor.integration.telegram.BotMessage;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

@Service
@RequiredArgsConstructor
public class BotMsgSender implements IBotSendMessage {
    private final TradevisorProperties trvProperties;
    @Override
    public void sendMessage(BotMessage message) {
        var chatId = trvProperties.integration().telegram().chatId();
        var chatToken = trvProperties.integration().telegram().chatToken();
        SendMessage sendMessage = SendMessage.builder()
                .allowSendingWithoutReply(true)
                .chatId(chatId)
                .text(message.title() + message.text())
                .build();
    }
}
