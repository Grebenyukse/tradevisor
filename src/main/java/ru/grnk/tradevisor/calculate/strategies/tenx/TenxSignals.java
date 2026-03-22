package ru.grnk.tradevisor.calculate.strategies.tenx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.tenx.enabled")
public class TenxSignals implements IStrategy {

    private final TradevisorProperties tradevisorProperties;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return tradevisorProperties.calculate().tenx().barsRequired();
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        int lookbackPeriod = tradevisorProperties.calculate().tenx().lookBackBars();
        double growthFactor = tradevisorProperties.calculate().tenx().growthFactor(); // например, 10.0
        double priceMultiplier = tradevisorProperties.calculate().tenx().priceMultiplier(); // например, 15.0
        double slMultiplier = tradevisorProperties.calculate().tenx().stopLossMultiplier(); // например, 10.0
        return TenxSignalsProducer.getTenxSignals(candles, lookbackPeriod, growthFactor, priceMultiplier, slMultiplier);
    }


    @Override
    public String getStrategyUniqueName() {
        return "tenx";
    }
}
