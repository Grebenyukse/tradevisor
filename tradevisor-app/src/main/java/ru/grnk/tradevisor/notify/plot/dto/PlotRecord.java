package ru.grnk.tradevisor.notify.plot.dto;

import java.util.List;

public record PlotRecord(
        List<OHLCData> data,
        HorizontalLineDto stopLoss,
        HorizontalLineDto takeProfit,
        HorizontalLineDto priceOpen,
        String ticker,
        String uuid,
        Short direction
) {
}
