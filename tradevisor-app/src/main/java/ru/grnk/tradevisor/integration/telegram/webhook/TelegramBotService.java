package ru.grnk.tradevisor.integration.telegram.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    public void processUpdate(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleMessage(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();

        switch (text) {
            case "/start":
                sendMenu(chatId);
                break;
            case "/menu":
                sendMenu(chatId);
                break;
            case "/stats":
                sendSimpleMessage(chatId, "пользователь запросил статистику");
                break;
            case "/account_info":
                sendSimpleMessage(chatId, "пользователь запросил информацию об аккаунте");
                break;
            case "/close_all":
                sendSimpleMessage(chatId, "пользователь запросил закрытие всех открытых позиций");
                break;
            default:
                sendWelcomeMessage(chatId);
                break;
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Message message = callbackQuery.getMessage();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        switch (callbackData) {
            case "accept":
                showReaction(chatId, messageId, "✅");
                break;
            case "decline":
                showReaction(chatId, messageId, "❌");
                break;
            case "stats":
                sendSimpleMessage(chatId, "пользователь запросил статистику");
                break;
            case "account_info":
                sendSimpleMessage(chatId, "пользователь запросил информацию об аккаунте");
                break;
            case "close_all":
                sendSimpleMessage(chatId, "пользователь запросил закрытие всех открытых позиций");
                break;
        }
    }

    public void sendMessage(Long chatId, String url, String title, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("<b>" + title + "</b>\n\n" + text + "\n\n" + url);
        sendMessage.enableHtml(true);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        
        InlineKeyboardButton acceptButton = new InlineKeyboardButton();
        acceptButton.setText("Accept");
        acceptButton.setCallbackData("accept");
        
        InlineKeyboardButton declineButton = new InlineKeyboardButton();
        declineButton.setText("Decline");
        declineButton.setCallbackData("decline");

        row.add(acceptButton);
        row.add(declineButton);
        rows.add(row);

        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        telegramApiClient.sendMessage(sendMessage);
    }

    private void showReaction(Long chatId, Integer messageId, String reaction) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(reaction);

        telegramApiClient.editMessageText(editMessage);
    }

    private void sendMenu(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("Выберите действие:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("/stats");
        statsButton.setCallbackData("stats");
        row1.add(statsButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton accountButton = new InlineKeyboardButton();
        accountButton.setText("/account_info");
        accountButton.setCallbackData("account_info");
        row2.add(accountButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton closeAllButton = new InlineKeyboardButton();
        closeAllButton.setText("/close_all");
        closeAllButton.setCallbackData("close_all");
        row3.add(closeAllButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        telegramApiClient.sendMessage(sendMessage);
    }

    private void sendSimpleMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        telegramApiClient.sendMessage(sendMessage);
    }

    private void sendWelcomeMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("Добро пожаловать! Используйте /menu для открытия меню.");
        telegramApiClient.sendMessage(sendMessage);
    }

    public String setWebhook() {
        return telegramApiClient.setWebhook();
    }
}
