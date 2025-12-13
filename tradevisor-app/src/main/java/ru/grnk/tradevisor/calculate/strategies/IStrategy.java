package ru.grnk.tradevisor.calculate.strategies;

import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.List;

public interface IStrategy {

    Integer barsRequiredToCalcStrategy();

    TrvCalculationResult calculate(List<MarketData> candles);

    String getStrategyUniqueName();

}
