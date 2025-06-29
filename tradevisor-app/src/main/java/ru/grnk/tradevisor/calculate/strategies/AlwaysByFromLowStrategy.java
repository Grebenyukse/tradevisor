package ru.grnk.tradevisor.calculate.strategies;

import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

@Component
public class  AlwaysByFromLowStrategy implements IStrategy {

    public static final String ALWAYS_BUY_ON_LOW_STRATEGY = "ALWAYS_BUY_ON_LOW_STRATEGY";

    @Override
    public Integer historyDepthBarsBy1m() {
        return 3;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        if (candles.get(0).getHigh() > candles.get(2).getHigh()) {
            return new TrvCalculationResult(
                    TradingDirection.LONG,
                    candles.get(1).getLow(),
                    null,
                    candles.get(1).getLow() + 0.5f, // фиксированный TP
                    1
            );
        } else {
            return new TrvCalculationResult(
                    TradingDirection.UNKNOWN, null, null, null, null);
        }
    }


    @Override
    public String getStrategyUniqueName() {
        return ALWAYS_BUY_ON_LOW_STRATEGY;
    }
}
