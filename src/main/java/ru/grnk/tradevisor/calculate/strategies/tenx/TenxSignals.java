package ru.grnk.tradevisor.calculate.strategies.tenx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.Marker;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.notify.plot.dto.ChartLineDto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.grnk.tradevisor.common.util.MathUtils.round;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.tenx.enabled")
public class TenxSignals implements IStrategy {

    private final TradevisorProperties tradevisorProperties;
    private final TickersRepository tickersRepository;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return tradevisorProperties.calculate().tenx().barsRequired();
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        var defaultNoSignal = TrvCalculationResult.builder()
                .direction(TradingDirection.UNKNOWN)
                .build();

        if (candles.isEmpty()) {
            log.warn("No candles provided.");
            return defaultNoSignal;
        }

        int lookbackPeriod = tradevisorProperties.calculate().tenx().lookBackBars();
        if (candles.size() < lookbackPeriod) {
            log.warn("Not enough candles for TenX strategy calculation.");
            return defaultNoSignal;
        }

        // Ищем минимум и его индекс
        MarketData minCandle = null;
        int minIndex = -1;
        float minValue = Float.MAX_VALUE;

        for (int i = 0; i < lookbackPeriod; i++) {
            MarketData candle = candles.get(i);
            if (candle.getLow() < minValue) {
                minValue = candle.getLow();
                minCandle = candle;
                minIndex = i;
            }
        }

        if (minCandle == null || minIndex == -1) {
            log.warn("Could not determine minimum candle in TenX strategy.");
            return defaultNoSignal;
        }

        // Ищем максимум после минимума
        MarketData maxCandle = null;
        float maxValue = -Float.MAX_VALUE;

        for (int i = minIndex + 1; i < candles.size(); i++) {
            MarketData candle = candles.get(i);
            if (candle.getHigh() > maxValue) {
                maxValue = candle.getHigh();
                maxCandle = candle;
            }
        }

        if (maxCandle == null) {
            log.warn("Could not determine maximum after the minimum in TenX strategy.");
            return defaultNoSignal;
        }

        double growthFactor = tradevisorProperties.calculate().tenx().growthFactor(); // например, 10.0
        double priceMultiplier = tradevisorProperties.calculate().tenx().priceMultiplier(); // например, 15.0
        double slMultiplier = tradevisorProperties.calculate().tenx().stopLossMultiplier(); // например, 10.0

        double ratio = maxValue / minValue;

        if (ratio >= growthFactor) {
            float priceOpen = (float) (minValue * priceMultiplier); // Цена входа
            float stopLoss = (float) (maxValue * slMultiplier);     // Stop Loss
            float takeProfit = minValue;                           // Take Profit

            List<ChartLineDto> lines = buildChartLines(
                    candles,
                    minCandle,
                    maxCandle,
                    priceOpen,
                    stopLoss,
                    takeProfit
            );

            log.info("TenX signal detected: Ratio={}. Entry at {}, SL={}, TP={}",
                    ratio, priceOpen, stopLoss, takeProfit);

            return TrvCalculationResult.builder()
                    .direction(TradingDirection.SHORT)
                    .priceOpen(priceOpen)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .lines(lines)
                    .build();
        }

        return defaultNoSignal;
    }

    private List<ChartLineDto> buildChartLines(
            List<MarketData> candles,
            MarketData minCandle,
            MarketData maxCandle,
            float priceOpen,
            float stopLoss,
            float takeProfit) {

        List<ChartLineDto> lines = new ArrayList<>();

        OffsetDateTime fromTime = candles.get(0).getTime();
        OffsetDateTime toTime = candles.get(candles.size() - 1).getTime();

        // Min line (Take Profit)
        lines.add(ChartLineDto.builder()
                .fromPrice(minCandle.getLow())
                .toPrice(minCandle.getLow())
                .fromUtc(minCandle.getTime())
                .toUtc(toTime)
                .style("solid")
                .label("Min (TP): " + round(minCandle.getLow(), 4))
                .color("green")
                .build());

        // Max line
        lines.add(ChartLineDto.builder()
                .fromPrice(maxCandle.getHigh())
                .toPrice(maxCandle.getHigh())
                .fromUtc(maxCandle.getTime())
                .toUtc(toTime)
                .style("dashed")
                .label("Max: " + round(maxCandle.getHigh(), 4))
                .color("orange")
                .build());

        // Price Open
        lines.add(ChartLineDto.builder()
                .fromPrice(priceOpen)
                .toPrice(priceOpen)
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("Entry: " + round(priceOpen, 4))
                .color("blue")
                .build());

        // Stop Loss
        lines.add(ChartLineDto.builder()
                .fromPrice(stopLoss)
                .toPrice(stopLoss)
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("SL: " + round(stopLoss, 4))
                .color("red")
                .build());

        return lines;
    }

    @Override
    public String getStrategyUniqueName() {
        return "tenx";
    }
}
