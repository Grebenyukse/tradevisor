package ru.grnk.tradevisor.common.properties;

public record TrvTelegramProperties(
        Boolean enabled,
        String chatId,
        String chatToken,
        String baseUrl
) {
}
