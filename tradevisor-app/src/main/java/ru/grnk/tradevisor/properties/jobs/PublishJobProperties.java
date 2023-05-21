package ru.grnk.tradevisor.properties.jobs;

import javax.validation.constraints.NotNull;
import java.time.Duration;

public record PublishJobProperties(
        @NotNull Duration interval,
        @NotNull Boolean enabled
) {
}
