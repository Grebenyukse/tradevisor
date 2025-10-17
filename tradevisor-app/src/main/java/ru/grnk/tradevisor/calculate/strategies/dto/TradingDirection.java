package ru.grnk.tradevisor.calculate.strategies.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum TradingDirection {

    LONG((short) 1, "buy"),
    SHORT((short)-1, "sell"),
    UNKNOWN((short)0, "no signal"),
    ;

    private final short directionCode;
    private final String description;

    public static TradingDirection from(Integer direction) {
        if (direction == null) {
            return UNKNOWN;
        }
        return switch (direction) {
            case 1 -> LONG;
            case -1 -> SHORT;
            default -> UNKNOWN;
        };
    }
    public static TradingDirection from(Short directionCode) {
        if (directionCode == null) {
            return UNKNOWN;
        }

        return switch (directionCode) {
            case 1 -> LONG;
            case -1 -> SHORT;
            default -> UNKNOWN;
        };
    }

}
