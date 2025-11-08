package ru.grnk.tradevisor.calculate.strategies.fibo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.List;

import static ru.grnk.tradevisor.calculate.strategies.fibo.FiboSignalsProducer.getFiboSignals;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.fibo")
public class FiboSignals implements IStrategy {


    private final TradevisorProperties tradevisorProperties;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return tradevisorProperties.calculate().barsRequiredToCalculateFibo();
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return getFiboSignals(candles, tradevisorProperties.calculate().minTouchesCount())
                .orElse(TrvCalculationResult.builder()
                        .direction(TradingDirection.UNKNOWN)
                        .build());
    }

    @Override
    public String getStrategyUniqueName() {
        return "fibo";
    }


}
