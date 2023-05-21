package ru.grnk.tradevisor.zcommon.properties.jobs;

import javax.validation.constraints.NotNull;

public record JobsProperties(
        @NotNull CalculateJobProperties calculate,
        @NotNull CalendarJobProperties calendar,
        @NotNull ControlJobProperties control,
        @NotNull NewsJobProperties news,
        @NotNull PricesJobProperties prices,
        @NotNull PublishJobProperties publish
) {
}
