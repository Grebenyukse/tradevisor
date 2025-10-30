package ru.grnk.tradevisor.notify.plot.dto;

import java.util.List;

public record PlotRecord(
        List<OHLCData> data,
        ChartLineDto stopLoss,
        ChartLineDto takeProfit,
        ChartLineDto priceOpen,
        String ticker,
        String uuid,
        Short direction,
        List<ChartLineDto> lines
) {
}
