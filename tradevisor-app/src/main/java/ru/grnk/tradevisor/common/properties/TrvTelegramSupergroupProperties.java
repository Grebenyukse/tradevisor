package ru.grnk.tradevisor.common.properties;

public record TrvTelegramSupergroupProperties(
        Long chatId,
        Integer positionsThreadId,
        Integer ordersThreadId,
        Integer rusThreadId,
        Integer worldThreadId,
        Integer cryptoThreadId,
        Integer logsThreadId,
        Integer errorsThreadId,
        Integer statisticsThreadId
) {}
