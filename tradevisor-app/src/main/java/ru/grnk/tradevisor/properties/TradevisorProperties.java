package ru.grnk.tradevisor.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.grnk.tradevisor.properties.jobs.JobsProperties;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
@Validated
public record TradevisorProperties(
        @NotNull JobsProperties jobs
) {
}
