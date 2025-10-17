package ru.grnk.tradevisor.calculate.strategies.fibo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.Marker;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(value = "app.calculate.fibo")
public class FiboSignals implements IStrategy {

    private static final int MIN_DATA_SIZE = 20;
    private static final int DEFAULT_BARS_REQUIRED = 100;
    private static final double FIBO_382_LEVEL = 0.382;
    private static final double FIBO_618_LEVEL = 0.618;
    private static final double SIGMA_MULTIPLIER = 0.03;
    private static final double ALPHA_MULTIPLIER = 0.01;
    private static final int TOUCH_SKIP_INTERVAL = 2;
    private static final int MIN_TOUCHES = 2;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return DEFAULT_BARS_REQUIRED;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return getFiboSignals(candles).orElse(new TrvCalculationResult(
                TradingDirection.UNKNOWN, null, null, null, null, List.of()));
    }

    @Override
    public String getStrategyUniqueName() {
        return "fibo";
    }

    public static Optional<TrvCalculationResult> getFiboSignals(List<MarketData> data) {
        List<MarketData> tickerData = data.size() > DEFAULT_BARS_REQUIRED ?
                data.subList(0, DEFAULT_BARS_REQUIRED) : data;

        if (tickerData.size() < MIN_DATA_SIZE) {
            return Optional.empty();
        }

        ExtremumResult extremums = findExtremums(tickerData);
        if (extremums.leftEBar() == extremums.rightEBar()) {
            return Optional.empty();
        }

        FiboLevels fiboLevels = calculateFiboLevels(extremums, tickerData);
        LevelAnalysisResult levelAnalysis = analyzeLevels(tickerData, extremums, fiboLevels);

        return generateSignal(tickerData, extremums, fiboLevels, levelAnalysis);
    }

    private static ExtremumResult findExtremums(List<MarketData> tickerData) {
        // Находим максимум
        Extremum supremum = IntStream.range(0, tickerData.size())
                .mapToObj(i -> new Extremum(tickerData.get(i).getHigh(), i))
                .max(Comparator.comparing(Extremum::value))
                .orElse(new Extremum(tickerData.get(0).getHigh(), 0));

        // Находим минимум
        Extremum infimum = IntStream.range(0, tickerData.size())
                .mapToObj(i -> new Extremum(tickerData.get(i).getLow(), i))
                .min(Comparator.comparing(Extremum::value))
                .orElse(new Extremum(tickerData.get(0).getLow(), 0));

        int leftEBar = Math.max(supremum.index(), infimum.index());
        int rightEBar = Math.min(supremum.index(), infimum.index());

        int trend = leftEBar == supremum.index() ? -1 : 1;
        float leftExtremum = leftEBar == supremum.index() ? supremum.value() : infimum.value();
        float rightExtremum = rightEBar == infimum.index() ? infimum.value() : supremum.value();

        return new ExtremumResult(supremum, infimum, leftEBar, rightEBar, trend, leftExtremum, rightExtremum);
    }

    private static FiboLevels calculateFiboLevels(ExtremumResult extremums, List<MarketData> tickerData) {
        float rangeSize = extremums.supremum().value() - extremums.infimum().value();
        float fibo382, fibo618;

        if (extremums.trend() == 1) {
            fibo382 = extremums.rightExtremum() - (float)(FIBO_382_LEVEL * rangeSize);
            fibo618 = extremums.rightExtremum() - (float)(FIBO_618_LEVEL * rangeSize);
        } else {
            fibo382 = extremums.rightExtremum() + (float)(FIBO_382_LEVEL * rangeSize);
            fibo618 = extremums.rightExtremum() + (float)(FIBO_618_LEVEL * rangeSize);
        }

        float sigma = rangeSize * (float)SIGMA_MULTIPLIER;
        float alpha = rangeSize * (float)ALPHA_MULTIPLIER;

        return new FiboLevels(fibo382, fibo618, rangeSize, sigma, alpha);
    }

    private static LevelAnalysisResult analyzeLevels(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels) {

        LevelCheckResult level382 = checkLevel(tickerData, extremums, fiboLevels, fiboLevels.fibo382(), true);
        LevelCheckResult level618 = checkLevel(tickerData, extremums, fiboLevels, fiboLevels.fibo618(), false);

        TouchCountResult touches382 = countTouches(tickerData, extremums, fiboLevels, fiboLevels.fibo382(), true);
        TouchCountResult touches618 = countTouches(tickerData, extremums, fiboLevels, fiboLevels.fibo618(), false);

        return new LevelAnalysisResult(
                level382.isBroken(), level618.isBroken(),
                touches382.touches(), touches618.touches(),
                touches382.markers(), touches618.markers()
        );
    }

    private static LevelCheckResult checkLevel(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels,
            float levelValue,
            boolean is382Level) {

        List<Marker> markers = new ArrayList<>();
        boolean isBroken = IntStream.range(0, extremums.rightEBar())
                .anyMatch(i -> {
                    boolean broken = isLevelBroken(tickerData.get(i), extremums.trend(), levelValue, fiboLevels.alpha());
                    if (broken) {
                        float markerValue = extremums.trend() == 1 ?
                                tickerData.get(i).getLow() : tickerData.get(i).getHigh();
                        markers.add(new Marker(i, markerValue, "black"));
                    }
                    return broken;
                });

        return new LevelCheckResult(isBroken, markers);
    }

    private static boolean isLevelBroken(MarketData data, int trend, float levelValue, float alpha) {
        if (trend == 1) {
            return data.getLow() < levelValue - alpha;
        } else {
            return data.getHigh() > levelValue + alpha;
        }
    }

    private static TouchCountResult countTouches(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels,
            float levelValue,
            boolean is382Level) {

        List<Marker> markers = new ArrayList<>();
        int touches = 0;

        for (int j = 0; j < extremums.rightEBar(); j++) {
            boolean touched = isLevelTouched(tickerData.get(j), extremums.trend(), levelValue, fiboLevels.sigma());
            if (touched) {
                touches++;
                j += TOUCH_SKIP_INTERVAL;
                if (j < tickerData.size()) {
                    markers.add(new Marker(j, levelValue, "black"));
                }
            }
        }

        return new TouchCountResult(touches, markers);
    }

    private static boolean isLevelTouched(MarketData data, int trend, float levelValue, float sigma) {
        if (trend == 1) {
            return Math.abs(data.getLow() - levelValue) < sigma;
        } else {
            return Math.abs(levelValue - data.getHigh()) < sigma;
        }
    }

    private static Optional<TrvCalculationResult> generateSignal(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels,
            LevelAnalysisResult analysis) {

        float stopLoss = extremums.trend() == 1 ? extremums.infimum().value() : extremums.supremum().value();
        float takeProfit = fiboLevels.fibo618();

        // Проверяем сигнал по уровню 38.2
        if (!analysis.isBroken382() && analysis.touches382() >= MIN_TOUCHES) {
            float priceOpen = (stopLoss + fiboLevels.fibo382()) / 2;

            List<HorizontalLineDto> lines = createHorizontalLines(
                    tickerData, extremums, fiboLevels, analysis.markers382());

            return Optional.of(TrvCalculationResult.builder()
                    .direction(TradingDirection.from(extremums.trend()))
                    .priceOpen(priceOpen)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .lots(analysis.touches382())
                    .lines(lines)
                    .build());
        }

        // Проверяем сигнал по уровню 61.8
        if (!analysis.isBroken618() && analysis.touches618() >= MIN_TOUCHES) {
            float priceOpen = fiboLevels.fibo382();

            List<HorizontalLineDto> lines = createHorizontalLines(
                    tickerData, extremums, fiboLevels, analysis.markers618());

            return Optional.of(TrvCalculationResult.builder()
                    .direction(TradingDirection.from(extremums.trend()))
                    .priceOpen(priceOpen)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .lots(analysis.touches618())
                    .lines(lines)
                    .build());
        }

        return Optional.empty();
    }

    private static List<HorizontalLineDto> createHorizontalLines(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels,
            List<Marker> markers) {

        OffsetDateTime fromTime = tickerData.get(extremums.rightEBar()).getTime();
        OffsetDateTime toTime = tickerData.get(tickerData.size() - 1).getTime();

        List<HorizontalLineDto> lines = new ArrayList<>();

        // Линия уровня 38.2
        lines.add(HorizontalLineDto.builder()
                .fromPrice(fiboLevels.fibo382())
                .toPrice(fiboLevels.fibo382())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("dashed")
                .label("Fibo 38.2")
                .color("#FF0000")
                .build());

        // Линия уровня 61.8
        lines.add(HorizontalLineDto.builder()
                .fromPrice(fiboLevels.fibo618())
                .toPrice(fiboLevels.fibo618())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("dashed")
                .label("Fibo 61.8")
                .color("#0000FF")
                .build());

        // Линии максимума и минимума
        lines.add(HorizontalLineDto.builder()
                .fromPrice(extremums.supremum().value())
                .toPrice(extremums.supremum().value())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("Supremum")
                .color("#00FF00")
                .build());

        lines.add(HorizontalLineDto.builder()
                .fromPrice(extremums.infimum().value())
                .toPrice(extremums.infimum().value())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("Infimum")
                .color("#FFFF00")
                .build());

        return lines;
    }

    private static String getPositionInfo(Float priceOpen, Float takeProfit, Float stopLoss) {
        Float tpToSl = Math.abs((priceOpen - takeProfit) / (priceOpen - stopLoss));
        return "\r\n PriceOpen:" + String.format("%.4f", priceOpen) +
                ". \r\n SL:" + String.format("%.4f", stopLoss) +
                ". \r\n TP:" + String.format("%.4f", takeProfit) +
                ". \r\n Kprofit:" + String.format("%.2f", tpToSl);
    }

    // Record classes для структурирования данных
    private record Extremum(float value, int index) {}

    private record ExtremumResult(
            Extremum supremum,
            Extremum infimum,
            int leftEBar,
            int rightEBar,
            int trend,
            float leftExtremum,
            float rightExtremum
    ) {}

    private record FiboLevels(float fibo382, float fibo618, float rangeSize, float sigma, float alpha) {}

    private record LevelCheckResult(boolean isBroken, List<Marker> markers) {}

    private record TouchCountResult(int touches, List<Marker> markers) {}

    private record LevelAnalysisResult(
            boolean isBroken382,
            boolean isBroken618,
            int touches382,
            int touches618,
            List<Marker> markers382,
            List<Marker> markers618
    ) {}
}
