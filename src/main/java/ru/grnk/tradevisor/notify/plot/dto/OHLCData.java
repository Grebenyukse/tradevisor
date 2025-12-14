package ru.grnk.tradevisor.notify.plot.dto;

import java.time.OffsetDateTime;

public record OHLCData(
        OffsetDateTime date,
        Float open,
        Float high,
        Float low,
        Float close) {
}