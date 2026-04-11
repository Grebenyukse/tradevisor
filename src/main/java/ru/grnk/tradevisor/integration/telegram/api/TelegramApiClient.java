package ru.grnk.tradevisor.integration.telegram.api;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
public interface TelegramApiClient {

    String setWebhook();
    String sendDocument(Message originalMessage, String fileContent, String filename, String caption);
    String setWebhookIfNeeded() ;
    String getWebhookInfo();
    boolean deleteWebhook() ;
    Message sendAndGetMessage(SendMessage sendMessage) ;
    boolean sendMessage(SendMessage sendMessage);
    boolean editMessageText(EditMessageText editMessageText);
    boolean deleteMessage(DeleteMessage deleteMessage);
}
