package ru.grnk.tradevisor.calculate.strategies.dto;


public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots) {

}
