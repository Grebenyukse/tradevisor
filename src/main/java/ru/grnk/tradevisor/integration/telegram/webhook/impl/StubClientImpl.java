package ru.grnk.tradevisor.integration.telegram.webhook.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;


@Service
@Slf4j
@ConditionalOnProperty(name = "app.integration.telegram.enabled", havingValue = "false")
public class StubClientImpl implements TelegramApiClient {

    @Override
    public String setWebhook() {
        return "";
    }

    @Override
    public String sendDocument(Message originalMessage, String fileContent, String filename, String caption) {
        return "";
    }

    @Override
    public String setWebhookIfNeeded() {
        return "";
    }

    @Override
    public String getWebhookInfo() {
        return "";
    }

    @Override
    public boolean deleteWebhook() {
        return false;
    }

    @Override
    public Message sendAndGetMessage(SendMessage sendMessage) {
        return null;
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
