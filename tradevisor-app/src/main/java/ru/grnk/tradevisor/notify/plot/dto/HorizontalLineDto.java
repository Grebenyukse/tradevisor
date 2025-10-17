package ru.grnk.tradevisor.notify.plot.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record HorizontalLineDto(
        double fromPrice,
        double toPrice,
        OffsetDateTime fromUtc,
        OffsetDateTime toUtc,
        String style,
        String label,
        String color
) {
}
