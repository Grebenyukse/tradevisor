package ru.grnk.tradevisor.calculate.strategies.gap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.*;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.gap")
public class GapSignals implements IStrategy {

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return 100;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return getGapSignals(candles).orElse(
                new TrvCalculationResult(TradingDirection.UNKNOWN, null, null, null, null, List.of())
        );
    }

    @Override
    public String getStrategyUniqueName() {
        return "gap";
    }

    public static Optional<TrvCalculationResult> getGapSignals(List<MarketData> data) {
        if (data.size() < 20) {
            return Optional.empty();
        }
        List<MarketData> ohlcRecord = data.subList(0, Math.min(100, data.size()));
        double rangeSize = ohlcRecord.stream()
                .mapToDouble(MarketData::getHigh).max().orElse(0) - ohlcRecord.stream().mapToDouble(MarketData::getLow).min().orElse(0);
        double minGapSize = rangeSize * 0.1;
        Double supremum = null;
        Double infimum = null;
        Integer gapBar = null;
        int trend = 0;
        List<Marker> markersTuplesInfimum = new ArrayList<>();
        List<Marker> markersTuplesSupremum = new ArrayList<>();
        for (int i = 0; i < ohlcRecord.size() - 1; i++) {
            double gap = ohlcRecord.get(i).getOpen() - ohlcRecord.get(i + 1).getClose();
            if (Math.abs(gap) > minGapSize) {
                if (gap > 0) {
                    supremum = Double.valueOf(ohlcRecord.get(i).getOpen());
                    infimum = Double.valueOf(ohlcRecord.get(i + 1).getClose());
                    trend = 1;
                } else {
                    supremum = Double.valueOf(ohlcRecord.get(i + 1).getClose());
                    infimum = Double.valueOf(ohlcRecord.get(i).getOpen());
                    trend = -1;
                }
                gapBar = i;
                break;
            }
        }
        if (gapBar == null || gapBar == 0) {
            return Optional.empty();
        }
        boolean gapIsBroken = false;
        double sigma = rangeSize * 0.03;
        double omega = rangeSize * 0.01;
        for (int j = 0; j < gapBar - 1; j++) {
            if (trend == -1) {
                if (ohlcRecord.get(j).getHigh() - supremum > omega) {
                    gapIsBroken = true;
                    markersTuplesSupremum.add(new Marker(j, ohlcRecord.get(j).getHigh(), "black"));
                }
            }
            if (trend == 1) {
                if (infimum - ohlcRecord.get(j).getLow() > omega) {
                    gapIsBroken = true;
                    markersTuplesInfimum.add(new Marker(j, ohlcRecord.get(j).getLow(), "black"));
                }
            }
        }

        if (gapIsBroken) {
            return Optional.empty();
        }

        int supremumTouches = 0;
        int infimumTouches = 0;
        int k = 0;

        while (k < gapBar) {
            if (trend == -1) {
                if (supremum - ohlcRecord.get(k).getHigh() < sigma) {
                    supremumTouches++;
                    k += 2;
                    markersTuplesSupremum.add(new Marker(k, ohlcRecord.get(k).getHigh(), "black"));
                }
            }
            if (trend == 1) {
                if (ohlcRecord.get(k).getLow() - infimum < sigma) {
                    infimumTouches++;
                    k += 2;
                    markersTuplesInfimum.add(new Marker(k, ohlcRecord.get(k).getLow(), "black"));
                }
            }
            k++;
        }

        if (supremumTouches > 1 || infimumTouches > 1) {
            Double takeProfit = null;
            Double stopLoss = null;
            Double priceOpen = null;

            if (supremumTouches > 1) {
                takeProfit = supremum;
                stopLoss = infimum - minGapSize;
                priceOpen = infimum;
                log.info("long signal detected. strategy: gap. touches: {}, stopLoss: {}, takeProfit: {}, PriceOpen: {}",
                        supremumTouches, stopLoss, takeProfit, priceOpen);
            }

            if (infimumTouches > 1) {
                takeProfit = infimum;
                stopLoss = supremum + minGapSize;
                priceOpen = supremum;
                log.info("short signal detected. strategy: gap. touches: {}, stopLoss: {}, takeProfit: {}, PriceOpen: {}",
                        infimumTouches, stopLoss, takeProfit, priceOpen);
            }
            return Optional.of(TrvCalculationResult.builder()
                    .lots(supremumTouches > 1 ? supremumTouches : infimumTouches)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .direction(TradingDirection.LONG)
                    .priceOpen(priceOpen)
                    .lines(List.of(
                            HorizontalLineDto.builder()
                                    .fromPrice(infimum)
                                    .toPrice(supremum)
                                    .fromUtc(data.get(gapBar).getTime())
                                    .toUtc(data.get(data.size()-1).getTime())
                                    .build()
                    ))
                    .build());
        }

        return Optional.empty();
    }
}

