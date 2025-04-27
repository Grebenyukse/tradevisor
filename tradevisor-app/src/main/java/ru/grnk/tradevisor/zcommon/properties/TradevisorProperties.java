package ru.grnk.tradevisor.zcommon.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
@Validated
public record TradevisorProperties(
        @NotNull TrvCollectProperties collect,
        @NotNull TrvCalculateProperties calculate,
        @NotNull TrvTradeProperties trade,
        @NotNull TrvJobProperties notification,
        @NotNull TrvIntegrationProperties integration

) {
}
