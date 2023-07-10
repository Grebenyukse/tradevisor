package ru.grnk.tradevisor.model.strategies;

import reactor.util.function.Tuple2;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.util.List;

public interface IStrategy {

    Integer historyDepthBarsBy1m();

    TrvCalculationResult calculate(List<MarketData> candles);

    String getStrategyUniqueName();

}
