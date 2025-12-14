package ru.grnk.tradevisor.integration.finam.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QuoteLevel {
    QUOTE_LEVEL_UNSPECIFIED(	0), //	Значение не указано
    QUOTE_LEVEL_LAST_PRICE	(1), //	Последняя цена
    QUOTE_LEVEL_BEST_BID_OFFER	(2), //	Бид аск
    QUOTE_LEVEL_DEPTH_OF_MARKET	(3), //	Агрегированный стакан
    QUOTE_LEVEL_DEPTH_OF_BOOK	(4), //	Полный стакан
    QUOTE_LEVEL_ACCESS_FORBIDDEN	(5), //	Доступ запрещен
    ;

    private final int value;
    public QuoteLevel valueOf(int val) {
        return switch (val) {
            case 0 -> QUOTE_LEVEL_UNSPECIFIED;
            case 1 -> QUOTE_LEVEL_LAST_PRICE;
            case 2 -> QUOTE_LEVEL_BEST_BID_OFFER;
            case 3 -> QUOTE_LEVEL_DEPTH_OF_MARKET;
            case 4 -> QUOTE_LEVEL_DEPTH_OF_BOOK;
            case 5 -> QUOTE_LEVEL_ACCESS_FORBIDDEN;
            default -> throw new RuntimeException("unknown quote level exception: " + val);
        };
    }
}
