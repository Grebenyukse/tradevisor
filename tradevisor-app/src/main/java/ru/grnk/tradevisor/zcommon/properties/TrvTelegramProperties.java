package ru.grnk.tradevisor.zcommon.properties;

public record TrvTelegramProperties(
        String chatId,
        String chatToken,
        String baseUrl
) {
}
