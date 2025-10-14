package ru.grnk.tradevisor.notify.publish.plot;

import java.time.OffsetDateTime;

public record PlotRecord(
        Float open,
        Float high,
        Float low,
        Float close,
        String ticker,
        String figi,
        String uuid,
        OffsetDateTime time
) {
}
