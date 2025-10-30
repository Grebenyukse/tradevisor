package ru.grnk.tradevisor.notify.plot.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ChartLineDto(
        Float fromPrice,
        Float toPrice,
        OffsetDateTime fromUtc,
        OffsetDateTime toUtc,
        String style,
        String label,
        String color
) {
}
