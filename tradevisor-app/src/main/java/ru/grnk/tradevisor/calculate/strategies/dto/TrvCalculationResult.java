package ru.grnk.tradevisor.calculate.strategies.dto;

import lombok.Builder;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.util.List;

@Builder
public record TrvCalculationResult(
        TradingDirection direction,
        Double priceOpen,
        Double stopLoss,
        Double takeProfit,
        Integer lots,
        List<HorizontalLineDto> lines) {
}
