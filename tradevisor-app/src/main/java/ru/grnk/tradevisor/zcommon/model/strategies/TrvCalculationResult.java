package ru.grnk.tradevisor.zcommon.model.strategies;


public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots) {

}
