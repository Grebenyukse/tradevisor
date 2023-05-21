package ru.grnk.tradevisor.properties.jobs;

import javax.validation.constraints.NotNull;
import java.time.Duration;

public record NewsJobProperties(
        @NotNull Duration interval,
        @NotNull Boolean enabled
) {
}
