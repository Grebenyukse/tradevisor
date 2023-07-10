package ru.grnk.tradevisor.model.strategies;

import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

public class ThreeBarGrowthStrategy implements IStrategy {

    @Override
    public Integer historyDepthBarsBy1m() {
        return 3;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        if (candles.get(0).getHigh() > candles.get(2).getHigh()) {
            return new TrvCalculationResult(
                    TradingDirection.LONG,
                    candles.get(0).getLow(),
                    candles.get(3).getLow(),
                    candles.get(0).getHigh(),
                    1
            );
        } else {
            return new TrvCalculationResult(
                    TradingDirection.UNKNOWN, null, null, null, null);
        }
    }


    @Override
    public String getStrategyUniqueName() {
        return "THREE_BARS_GROWTH_STRATEGY";
    }
}
