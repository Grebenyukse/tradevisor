package ru.grnk.tradevisor.integration.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class TelegramMessageBuilder {

    public static Integer getSignalIdFromQuery(String callbackData) {
        var parts = callbackData.split("/");
        if (parts.length < 2) {
            throw new RuntimeException("unkown command on button click query " + callbackData);
        }
        return Integer.parseInt(parts[1]);
    }

    public static DeleteMessage buildDeleteMessage(Message originalMessage) {
        DeleteMessage dm = new DeleteMessage();
        dm.setChatId(originalMessage.getChatId().toString());
        dm.setMessageId(originalMessage.getMessageId());
        return dm;
    }

    public static EditMessageText buildReactionMessage(Message originalMessage, String reaction) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(originalMessage.getChatId().toString());
        edit.setMessageId(originalMessage.getMessageId());
        String newText = reaction + "  " + originalMessage.getText();
        edit.setText(newText);
        edit.enableHtml(true);
        InlineKeyboardMarkup emptyMarkup = new InlineKeyboardMarkup();
        emptyMarkup.setKeyboard(new ArrayList<>());
        edit.setReplyMarkup(emptyMarkup);
        return edit;
    }

    public static SendMessage sendSimpleMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        return sendMessage;
    }

    public static SendMessage sendSimpleMessage(Long chatId, Integer threadId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setMessageThreadId(threadId);
        sendMessage.setText(text);
        return sendMessage;
    }

    public static SendMessage sendMenu(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("Выберите действие:");

        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("/stats");
        statsButton.setCallbackData("stats");

        InlineKeyboardButton accountButton = new InlineKeyboardButton();
        accountButton.setText("/account_info");
        accountButton.setCallbackData("account_info");

        InlineKeyboardButton closeAllButton = new InlineKeyboardButton();
        closeAllButton.setText("/close_all");
        closeAllButton.setCallbackData("close_all");

        InlineKeyboardButton resetSignals = new InlineKeyboardButton();
        resetSignals.setText("/delete_signals");
        resetSignals.setCallbackData("delete_signals");

        InlineKeyboardButton deleteAllMessages = new InlineKeyboardButton();
        deleteAllMessages.setText("/delete_all_messages");
        deleteAllMessages.setCallbackData("delete_all_messages");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(statsButton);
        row1.add(accountButton);
        row1.add(closeAllButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(resetSignals);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(deleteAllMessages);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);
        return sendMessage;
    }

    public static SendMessage sendWelcomeMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("Добро пожаловать! Используйте /menu для открытия меню.");
        return sendMessage;
    }

    public static SendMessage buildChartMessage(Long chatId, Integer threadId, String url, String title, String text, Integer signalId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setMessageThreadId(threadId);
        sendMessage.setText("<b>" + title + "</b>\n" + url + "\n" + text);
        sendMessage.enableHtml(true);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton acceptButton = new InlineKeyboardButton();
        acceptButton.setText("Accept");
        acceptButton.setCallbackData("accept/" + signalId);

        InlineKeyboardButton declineButton = new InlineKeyboardButton();
        declineButton.setText("Decline");
        declineButton.setCallbackData("decline/" + signalId);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Delete");
        deleteButton.setCallbackData("delete/" + signalId);
        // second row
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton downloadButton = new InlineKeyboardButton();
        downloadButton.setText("Download quotes");
        downloadButton.setCallbackData("download/" + signalId);

        InlineKeyboardButton eventsButton = new InlineKeyboardButton();
        eventsButton.setText("show events");
        eventsButton.setCallbackData("events/" + signalId);

        row.add(acceptButton);
        row.add(declineButton);
        row.add(deleteButton);

        row2.add(downloadButton);
        row2.add(eventsButton);
        rows.add(row);
        rows.add(row2);

        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);
        return sendMessage;
    }

}