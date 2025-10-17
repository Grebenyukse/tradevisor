package ru.grnk.tradevisor.notify.plot.dto;

import java.time.OffsetDateTime;

public record HorizontalLineDto(
        double price,
        OffsetDateTime from,
        OffsetDateTime to,
        String style,
        String label,
        String color
) {
}
