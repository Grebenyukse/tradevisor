package ru.grnk.tradevisor.bcalculate.strategies;


public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots) {

}
