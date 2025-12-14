package ru.grnk.tradevisor.integration.yahoofinance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooChartResponse(YahooChart chart) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChart(
            List<YahooChartResult> result,
            YahooChartError error
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChartError(
            String code,
            String description
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChartResult(
            List<Long> timestamp,
            YahooIndicators indicators,
            YahooMeta meta
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooIndicators(
            List<YahooQuote> quote
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooQuote(
            List<BigDecimal> open,
            List<BigDecimal> high,
            List<BigDecimal> low,
            List<BigDecimal> close,
            List<BigDecimal> volume
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooMeta(
            String currency,
            String symbol,
            String exchangeName,
            String fullExchangeName,
            String instrumentType,
            Long firstTradeDate,
            Long regularMarketTime,
            Boolean hasPrePostMarketData,
            Integer gmtoffset,
            String timezone,
            String exchangeTimezoneName,
            BigDecimal regularMarketPrice,
            BigDecimal fiftyTwoWeekHigh,
            BigDecimal fiftyTwoWeekLow,
            BigDecimal regularMarketDayHigh,
            BigDecimal regularMarketDayLow,
            Long regularMarketVolume,
            String longName,
            String shortName,
            BigDecimal chartPreviousClose,
            BigDecimal previousClose,
            Integer scale,
            Integer priceHint,
            YahooCurrentTradingPeriod currentTradingPeriod,
            List<List<YahooTradingPeriod>> tradingPeriods,
            String dataGranularity,
            String range,
            List<String> validRanges
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooCurrentTradingPeriod(
            YahooPeriodDetails pre,
            YahooPeriodDetails regular,
            YahooPeriodDetails post
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooPeriodDetails(
            String timezone,
            Long start,
            Long end,
            Integer gmtoffset
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooTradingPeriod(
            String timezone,
            Long start,
            Long end,
            Integer gmtoffset
    ) {}
}
