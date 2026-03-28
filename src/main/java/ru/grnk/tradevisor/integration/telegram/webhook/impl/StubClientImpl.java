package ru.grnk.tradevisor.integration.telegram.webhook.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;


@Service
@Slf4j
@ConditionalOnMissingBean(value = TelegramApiClientImpl.class)
public class StubClientImpl implements TelegramApiClient {

    @Override
    public String setWebhook() {
        return "stub setWebhook";
    }

    @Override
    public String sendDocument(Message originalMessage, String fileContent, String filename, String caption) {
        return "stub sendDocument";
    }

    @Override
    public String setWebhookIfNeeded() {
        return "stub setWebhookIfNeeded";
    }

    @Override
    public String getWebhookInfo() {
        return "stub getWebhookInfo";
    }

    @Override
    public boolean deleteWebhook() {
        return false;
    }

    @Override
    public Message sendAndGetMessage(SendMessage sendMessage) {
        var msg =  new Message();
        msg.setMessageId(-1);
        return msg;
    }

    @Override
    public boolean sendMessage(SendMessage sendMessage) {
        return false;
    }

    @Override
    public boolean editMessageText(EditMessageText editMessageText) {
        return false;
    }

    @Override
    public boolean deleteMessage(DeleteMessage deleteMessage) {
        return false;
    }
}
