package ru.grnk.tradevisor.properties;

import ru.grnk.tradevisor.properties.jobs.CalculateJobProperties;
import ru.grnk.tradevisor.properties.jobs.CalendarJobProperties;
import ru.grnk.tradevisor.properties.jobs.ControlJobProperties;
import ru.grnk.tradevisor.properties.jobs.NewsJobProperties;
import ru.grnk.tradevisor.properties.jobs.PricesJobProperties;
import ru.grnk.tradevisor.properties.jobs.PublishJobProperties;

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
