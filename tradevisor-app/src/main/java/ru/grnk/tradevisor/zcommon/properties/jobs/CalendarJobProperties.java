package ru.grnk.tradevisor.zcommon.properties.jobs;

import javax.validation.constraints.NotNull;
import java.time.Duration;

public record CalendarJobProperties(
        @NotNull Duration interval,
        @NotNull Boolean enabled
) {
}
