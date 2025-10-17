package ru.grnk.tradevisor.calculate.strategies.dto;

import lombok.Builder;

import java.util.Date;

@Builder
public record SignalResult(
        String ticker,
        Date datetime,
        String expert,
        int trend,
        int criteria,
        String description
) {

}