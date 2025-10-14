package ru.grnk.tradevisor.calculate.strategies.dto;

import java.util.Date;

public record OhlcRecord(
         String ticker,
         Date datetime,
         double open,
         double high,
         double low,
         double close
) {
}
