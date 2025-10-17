package ru.grnk.tradevisor.calculate.strategies.dto;

import java.util.Date;

public record OhlcRecord(
         String ticker,
         Date datetime,
         Float open,
         Float high,
         Float low,
         Float close
) {
}
