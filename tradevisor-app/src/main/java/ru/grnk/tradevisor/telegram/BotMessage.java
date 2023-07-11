package ru.grnk.tradevisor.telegram;

import java.util.List;

public record BotMessage(String title, String text, byte[] image, List<TelegramButton> buttons) {

}
