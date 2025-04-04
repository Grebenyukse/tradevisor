package ru.grnk.tradevisor.zcommon.model.strategies;

import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

public interface IStrategy {

    Integer historyDepthBarsBy1m();

    TrvCalculationResult calculate(List<MarketData> candles);

    String getStrategyUniqueName();

}
