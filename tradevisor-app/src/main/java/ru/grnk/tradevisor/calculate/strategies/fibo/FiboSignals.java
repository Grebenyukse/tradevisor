package ru.grnk.tradevisor.calculate.strategies.fibo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

import static ru.grnk.tradevisor.calculate.strategies.fibo.FiboSignalsProducer.getFiboSignals;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.fibo")
public class FiboSignals implements IStrategy {

    private static final int DEFAULT_BARS_REQUIRED = 100;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return DEFAULT_BARS_REQUIRED;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return getFiboSignals(candles)
                .orElse(TrvCalculationResult.builder()
                        .direction(TradingDirection.UNKNOWN)
                        .build());
    }

    @Override
    public String getStrategyUniqueName() {
        return "fibo";
    }


}
