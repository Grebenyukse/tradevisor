package ru.grnk.tradevisor.zcommon.model.strategies;

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

}
