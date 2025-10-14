package ru.grnk.tradevisor.calculate.strategies.dto;

import java.util.Date;

public record SignalResult(String ticker, Date datetime, String expert, int trend, int criteria, String description) {
}