package ru.grnk.tradevisor.calculate.strategies.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.calculate.convergence.enabled", havingValue = "true")
public class ConvergingChannelStrategy implements IStrategy {

    private final TradevisorProperties properties;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return properties.calculate().convergence().lookBackBars();
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return ConvergingChannelStrategyProducer.calculate(candles, barsRequiredToCalcStrategy());
    }

    @Override
    public String getStrategyUniqueName() {
        return "convergence";
    }
}
