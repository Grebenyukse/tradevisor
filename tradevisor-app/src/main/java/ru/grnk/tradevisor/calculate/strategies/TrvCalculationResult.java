package ru.grnk.tradevisor.calculate.strategies;


public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots) {

}
