package ru.grnk.tradevisor.calculate.strategies.gap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.Marker;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.gap")
public class GapSignals implements IStrategy {

    private static final int MIN_DATA_SIZE = 20;
    private static final int DEFAULT_BARS_REQUIRED = 100;
    private static final double GAP_SIZE_MULTIPLIER = 0.1;
    private static final double SIGMA_MULTIPLIER = 0.03;
    private static final double OMEGA_MULTIPLIER = 0.01;
    private static final int TOUCH_SKIP_INTERVAL = 2;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return DEFAULT_BARS_REQUIRED;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return getGapSignals(candles).orElse(
                TrvCalculationResult.builder()
                        .direction(TradingDirection.UNKNOWN)
                        .build());
    }

    @Override
    public String getStrategyUniqueName() {
        return "gap";
    }

    public static Optional<TrvCalculationResult> getGapSignals(List<MarketData> data) {
        if (data.size() < MIN_DATA_SIZE) {
            return Optional.empty();
        }

        List<MarketData> ohlcRecord = data.subList(0, Math.min(DEFAULT_BARS_REQUIRED, data.size()));
        double rangeSize = calculateRangeSize(ohlcRecord);
        double minGapSize = rangeSize * GAP_SIZE_MULTIPLIER;

        return findGap(ohlcRecord, minGapSize, rangeSize)
                .filter(gapInfo -> !isGapBroken(ohlcRecord, gapInfo, rangeSize))
                .flatMap(gapInfo -> processGapTouches(ohlcRecord, gapInfo, rangeSize, minGapSize, data));
    }

    private static double calculateRangeSize(List<MarketData> ohlcRecord) {
        double maxHigh = ohlcRecord.stream()
                .mapToDouble(MarketData::getHigh)
                .max()
                .orElse(0);
        double minLow = ohlcRecord.stream()
                .mapToDouble(MarketData::getLow)
                .min()
                .orElse(0);
        return maxHigh - minLow;
    }

    private static Optional<GapInfo> findGap(List<MarketData> ohlcRecord, double minGapSize, double rangeSize) {
        return IntStream.range(0, ohlcRecord.size() - 1)
                .mapToObj(i -> {
                    double gap = ohlcRecord.get(i).getOpen() - ohlcRecord.get(i + 1).getClose();
                    if (Math.abs(gap) > minGapSize) {
                        if (gap > 0) {
                            return new GapInfo(i, ohlcRecord.get(i).getOpen(), ohlcRecord.get(i + 1).getClose(), 1);
                        } else {
                            return new GapInfo(i, ohlcRecord.get(i + 1).getClose(), ohlcRecord.get(i).getOpen(), -1);
                        }
                    }
                    return null;
                })
                .filter(gapInfo -> gapInfo != null && gapInfo.gapBar() > 0)
                .findFirst();
    }

    private static boolean isGapBroken(List<MarketData> ohlcRecord, GapInfo gapInfo, double rangeSize) {
        double omega = rangeSize * OMEGA_MULTIPLIER;

        return IntStream.range(0, gapInfo.gapBar() - 1)
                .anyMatch(j -> {
                    if (gapInfo.trend() == -1) {
                        return ohlcRecord.get(j).getHigh() - gapInfo.supremum() > omega;
                    } else if (gapInfo.trend() == 1) {
                        return gapInfo.infimum() - ohlcRecord.get(j).getLow() > omega;
                    }
                    return false;
                });
    }

    private static Optional<TrvCalculationResult> processGapTouches(
            List<MarketData> ohlcRecord,
            GapInfo gapInfo,
            double rangeSize,
            double minGapSize,
            List<MarketData> originalData) {

        double touchRegistrationGap = rangeSize * SIGMA_MULTIPLIER;
        TouchCountResult touchResult = countTouches(ohlcRecord, gapInfo, touchRegistrationGap);

        if (touchResult.supremumTouches() > 1 || touchResult.infimumTouches() > 1) {
            SignalParams signalParams = calculateSignalParams(
                    touchResult.supremumTouches(),
                    touchResult.infimumTouches(),
                    gapInfo.supremum(),
                    gapInfo.infimum(),
                    minGapSize
            );

            logSignalDetection(touchResult, signalParams);

            return Optional.of(createCalculationResult(signalParams, gapInfo, originalData));
        }

        return Optional.empty();
    }

    private static TouchCountResult countTouches(List<MarketData> ohlcRecord, GapInfo gapInfo, double touchRegistrationGap) {
        List<Marker> markersTuplesInfimum = new ArrayList<>();
        List<Marker> markersTuplesSupremum = new ArrayList<>();
        int supremumTouches = 0;
        int infimumTouches = 0;

        for (int k = 0; k < gapInfo.gapBar(); k++) {
            if (gapInfo.trend() == -1) {
                if (gapInfo.supremum() - ohlcRecord.get(k).getHigh() < touchRegistrationGap) {
                    supremumTouches++;
                    k += TOUCH_SKIP_INTERVAL;
                    if (k < ohlcRecord.size()) {
                        markersTuplesSupremum.add(new Marker(k, ohlcRecord.get(k).getHigh(), "black"));
                    }
                }
            }
            if (gapInfo.trend() == 1) {
                if (ohlcRecord.get(k).getLow() - gapInfo.infimum() < touchRegistrationGap) {
                    infimumTouches++;
                    k += TOUCH_SKIP_INTERVAL;
                    if (k < ohlcRecord.size()) {
                        markersTuplesInfimum.add(new Marker(k, ohlcRecord.get(k).getLow(), "black"));
                    }
                }
            }
        }

        return new TouchCountResult(supremumTouches, infimumTouches);
    }

    private static SignalParams calculateSignalParams(
            int supremumTouches,
            int infimumTouches,
            double supremum,
            double infimum,
            double minGapSize) {

        if (supremumTouches > 1) {
            return new SignalParams(
                    supremum,                           // takeProfit
                    infimum - minGapSize,              // stopLoss
                    infimum,                           // priceOpen
                    supremumTouches,                   // lots
                    TradingDirection.LONG              // direction
            );
        } else {
            return new SignalParams(
                    infimum,                           // takeProfit
                    supremum + minGapSize,             // stopLoss
                    supremum,                          // priceOpen
                    infimumTouches,                    // lots
                    TradingDirection.SHORT             // direction
            );
        }
    }

    private static void logSignalDetection(TouchCountResult touchResult, SignalParams signalParams) {
        if (touchResult.supremumTouches() > 1) {
            log.info("long signal detected. strategy: gap. touches: {}, stopLoss: {}, takeProfit: {}, PriceOpen: {}",
                    touchResult.supremumTouches(), signalParams.stopLoss(), signalParams.takeProfit(), signalParams.priceOpen());
        }
        if (touchResult.infimumTouches() > 1) {
            log.info("short signal detected. strategy: gap. touches: {}, stopLoss: {}, takeProfit: {}, PriceOpen: {}",
                    touchResult.infimumTouches(), signalParams.stopLoss(), signalParams.takeProfit(), signalParams.priceOpen());
        }
    }

    private static TrvCalculationResult createCalculationResult(
            SignalParams signalParams,
            GapInfo gapInfo,
            List<MarketData> originalData) {

        return TrvCalculationResult.builder()
                .lots(signalParams.lots())
                .stopLoss((float)signalParams.stopLoss())
                .takeProfit((float)signalParams.takeProfit())
                .direction(signalParams.direction())
                .priceOpen((float)signalParams.priceOpen())
                .lines(List.of(
                        HorizontalLineDto.builder()
                                .fromPrice((float)gapInfo.infimum())
                                .toPrice((float)gapInfo.supremum())
                                .fromUtc(originalData.get(gapInfo.gapBar()).getTime())
                                .toUtc(originalData.get(originalData.size() - 1).getTime())
                                .build()
                ))
                .build();
    }

    private record GapInfo(int gapBar, double supremum, double infimum, int trend) {}

    private record TouchCountResult(int supremumTouches, int infimumTouches) {}

    private record SignalParams(double takeProfit, double stopLoss, double priceOpen, int lots, TradingDirection direction) {}
}
