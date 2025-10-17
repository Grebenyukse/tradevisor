package ru.grnk.tradevisor.calculate.strategies.alwaysbuy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

@Component
@ConditionalOnProperty(value = "app.calculate.always-buy")
public class  AlwaysByFromLowStrategy implements IStrategy {

    public static final String ALWAYS_BUY_ON_LOW_STRATEGY = "ALWAYS_BUY_ON_LOW_STRATEGY";

    @Override
    public Integer barsRequiredToCalcStrategy() {
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
                    1,
                    List.of()
            );
        } else {
            return new TrvCalculationResult(
                    TradingDirection.UNKNOWN, null, null, null, null, List.of());
        }
    }


    @Override
    public String getStrategyUniqueName() {
        return ALWAYS_BUY_ON_LOW_STRATEGY;
    }
}
