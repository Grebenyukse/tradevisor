package ru.grnk.tradevisor.calculate.strategies.dto;

import lombok.Builder;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.util.List;

@Builder
public record TrvCalculationResult(
        TradingDirection direction,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Integer lots,
        List<HorizontalLineDto> lines,
        String description) {
}
