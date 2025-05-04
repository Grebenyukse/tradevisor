package ru.grnk.tradevisor.zcommon.properties;

public record TrvTelegramProperties(
        Boolean enabled,
        String chatId,
        String chatToken,
        String baseUrl
) {
}
