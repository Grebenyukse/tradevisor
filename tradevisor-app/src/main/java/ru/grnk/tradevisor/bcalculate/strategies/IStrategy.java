package ru.grnk.tradevisor.bcalculate.strategies;

import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

public interface IStrategy {

    Integer historyDepthBarsBy1m();

    TrvCalculationResult calculate(List<MarketData> candles);

    String getStrategyUniqueName();

}
