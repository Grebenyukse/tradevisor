package ru.grnk.tradevisor.notify.plot.dto;

import java.time.OffsetDateTime;

public record OHLCData(
        OffsetDateTime date,
        double open,
        double high,
        double low,
        double close) {
}