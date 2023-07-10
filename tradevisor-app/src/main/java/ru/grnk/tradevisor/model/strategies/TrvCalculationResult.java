package ru.grnk.tradevisor.model.strategies;


public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots) {

}
