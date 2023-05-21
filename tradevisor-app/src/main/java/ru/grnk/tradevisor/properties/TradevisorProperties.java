package ru.grnk.tradevisor.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
@Validated
public record TradevisorProperties(
        @NotNull JobsProperties jobs
) {
}
