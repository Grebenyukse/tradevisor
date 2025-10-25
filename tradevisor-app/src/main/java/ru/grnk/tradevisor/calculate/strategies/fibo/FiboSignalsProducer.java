package ru.grnk.tradevisor.calculate.strategies.fibo;

import lombok.extern.slf4j.Slf4j;
import ru.grnk.tradevisor.calculate.strategies.dto.Marker;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class FiboSignalsProducer {
    private static final int MIN_DATA_SIZE = 20;
    private static final double FIBO_382_LEVEL = 0.382;
    private static final double FIBO_618_LEVEL = 0.618;
    private static final double TOUCH_REGISTRATION_GAP_MULTIPLIER = 0.03;
    private static final double LEVEL_BREAKDOWN_GAP_MULTIPLIER = 0.01;
    private static final int TOUCH_SKIP_INTERVAL = 2;
    private static final int MIN_TOUCHES = 2;

    public static Optional<TrvCalculationResult> getFiboSignals(List<MarketData> tickerData) {
        if (tickerData.size() < MIN_DATA_SIZE) {
            return Optional.empty();
        }
        ExtremumResult extremums = findExtremums(tickerData);
        if (extremums.left().index() == extremums.right().index()) {
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

        // массив обходим справа-налево. 0-первый бар, 100- последний. определяем правый и левый экстремум
        Extremum left;
        Extremum right;
        int trend;
        if (infimum.index() > supremum.index()) {
            left = infimum;
            right = supremum;
            trend = 1;
        } else {
            left = supremum;
            right = infimum;
            trend = -1;
        }
        return new ExtremumResult(left, right, trend);
    }

    private static FiboLevels calculateFiboLevels(ExtremumResult extremums, List<MarketData> tickerData) {
        float rangeSize = extremums.trend() * (extremums.right().value() - extremums.left().value());
        float fibo382, fibo618;
        if (extremums.trend() == 1) {
            fibo382 = extremums.right().value() - (float) (FIBO_382_LEVEL * rangeSize);
            fibo618 = extremums.right().value() - (float) (FIBO_618_LEVEL * rangeSize);
        } else {
            fibo382 = extremums.right().value() + (float) (FIBO_382_LEVEL * rangeSize);
            fibo618 = extremums.right().value() + (float) (FIBO_618_LEVEL * rangeSize);
        }
        float touchRegistrationGap = rangeSize * (float) TOUCH_REGISTRATION_GAP_MULTIPLIER;
        float levelBreakdownGap = rangeSize * (float) LEVEL_BREAKDOWN_GAP_MULTIPLIER;
        return new FiboLevels(fibo382, fibo618, rangeSize, touchRegistrationGap, levelBreakdownGap);
    }

    private static LevelAnalysisResult analyzeLevels(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels) {
        LevelCheckResult level382 = checkLevel(tickerData, extremums, fiboLevels, fiboLevels.fibo382());
        LevelCheckResult level618 = checkLevel(tickerData, extremums, fiboLevels, fiboLevels.fibo618());
        TouchCountResult touches382 = countTouches(tickerData, extremums, fiboLevels.touchRegistrationGap(), fiboLevels.fibo382());
        TouchCountResult touches618 = countTouches(tickerData, extremums, fiboLevels.touchRegistrationGap(), fiboLevels.fibo618());
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
            float levelValue) {
        List<Marker> markers = new ArrayList<>();
        boolean isBroken = IntStream.range(0, extremums.right().index())
                .anyMatch(i -> {
                    boolean broken = isLevelBroken(tickerData.get(i), extremums.trend(), levelValue, fiboLevels.levelBreakdownGap());
                    if (broken) {
                        float markerValue = extremums.trend() == 1 ?
                                tickerData.get(i).getLow() : tickerData.get(i).getHigh();
                        markers.add(new Marker(i, markerValue, "black"));
                    }
                    return broken;
                });
        return new LevelCheckResult(isBroken, markers);
    }

    private static boolean isLevelBroken(MarketData data, int trend, float levelValue, float levelBreakdownGap) {
        if (trend == 1) {
            // на растущем тренде проверяем пробой фибо сверху вниз
            return data.getLow() < levelValue - levelBreakdownGap;
        } else {
            // на падающем тренде проверяем пробой фибо снизу вверх
            return data.getHigh() > levelValue + levelBreakdownGap;
        }
    }

    private static TouchCountResult countTouches(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            float touchRegistrationGap,
            float levelValue) {
        List<Marker> markers = new ArrayList<>();
        int touches = 0;
        for (int j = 0; j < extremums.right().index(); j++) {
            boolean touched = isLevelTouched(tickerData.get(j), extremums.trend(), levelValue, touchRegistrationGap);
            if (touched) {
                touches++;
                if (j < tickerData.size()) {
                    markers.add(new Marker(j, levelValue, "blue"));
                }
                j += TOUCH_SKIP_INTERVAL;
            }
        }

        return new TouchCountResult(touches, markers);
    }

    private static boolean isLevelTouched(MarketData data, int trend, float levelValue, float touchRegistrationGap) {
        if (trend == 1) {
            // на растущем тренде проверяем касание сверху вниз. расстояние до минимума должно быть меньше сигмы.
            return Math.abs(data.getLow() - levelValue) < touchRegistrationGap;
        } else {
            // на растущем тренде проверяем касание снизу вверх. расстояние до максимума должно быть меньше сигмы.
            return Math.abs(levelValue - data.getHigh()) < touchRegistrationGap;
        }
    }

    private static Optional<TrvCalculationResult> generateSignal(
            List<MarketData> tickerData,
            ExtremumResult extremums,
            FiboLevels fiboLevels,
            LevelAnalysisResult analysis) {
        float stopLoss = extremums.right().value();
        float takeProfit = fiboLevels.fibo618();
        if (!analysis.isBroken382() && analysis.touches382() >= MIN_TOUCHES) {
            float priceOpen = (extremums.right().value() + fiboLevels.fibo382()) / 2;
            List<HorizontalLineDto> lines = createHorizontalLines(
                    tickerData, extremums, fiboLevels, analysis.markers382());
            log.info("обнаружен сигнал по стратегии FIBO. {} касания 38.2. {}",analysis.touches382(),
                    getPositionInfo(priceOpen, takeProfit, stopLoss));
            return Optional.of(TrvCalculationResult.builder()
                    .direction(TradingDirection.from(-1 * extremums.trend())) // контртрендовая страта. позиция открывается на продолжение коррекции
                    .priceOpen(priceOpen)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .lots(analysis.touches382())
                    .lines(lines)
                    .build());
        }
        if (!analysis.isBroken618() && analysis.touches618() >= MIN_TOUCHES) {
            float priceOpen = fiboLevels.fibo382();
            List<HorizontalLineDto> lines = createHorizontalLines(
                    tickerData, extremums, fiboLevels, analysis.markers618());
            log.info("обнаружен сигнал по стратегии FIBO. {} касания 61.8. {}",analysis.touches382(),
                    getPositionInfo(priceOpen, takeProfit, stopLoss));
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
        OffsetDateTime fromTime = tickerData.get(extremums.right().index()).getTime();
        OffsetDateTime toTime = tickerData.get(tickerData.size() - 1).getTime();
        List<HorizontalLineDto> lines = new ArrayList<>();
        lines.add(HorizontalLineDto.builder()
                .fromPrice(fiboLevels.fibo382())
                .toPrice(fiboLevels.fibo382())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("dashed")
                .label("Fibo 38.2")
                .color("#FF0000")
                .build());
        lines.add(HorizontalLineDto.builder()
                .fromPrice(fiboLevels.fibo618())
                .toPrice(fiboLevels.fibo618())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("dashed")
                .label("Fibo 61.8")
                .color("#0000FF")
                .build());
        lines.add(HorizontalLineDto.builder()
                .fromPrice(extremums.right().value())
                .toPrice(extremums.right().value())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("Right")
                .color("#00FF00")
                .build());
        lines.add(HorizontalLineDto.builder()
                .fromPrice(extremums.left().value())
                .toPrice(extremums.left().value())
                .fromUtc(fromTime)
                .toUtc(toTime)
                .style("solid")
                .label("Left")
                .color("#FFFF00")
                .build());
        for (var marker : markers) {
            lines.add(HorizontalLineDto.builder()
                    .fromPrice(marker.y())
                    .toPrice(marker.y())
                    .color(marker.color())
                    .fromUtc(tickerData.get(marker.x()).getTime())
                    .toUtc(tickerData.get(marker.x()).getTime())
                    .label("Touch")
                    .style("round")
                    .build());
        }
        return lines;
    }

    private static String getPositionInfo(Float priceOpen, Float takeProfit, Float stopLoss) {
        if (priceOpen == null || takeProfit == null || stopLoss == null) {
            return "";
        }

        // Расчет абсолютных значений
        float slPoints = Math.abs(priceOpen - stopLoss);
        float tpPoints = Math.abs(priceOpen - takeProfit);

        // Расчет процентных значений (относительно цены открытия)
        float slPercent = (slPoints / priceOpen) * 100;
        float tpPercent = (tpPoints / priceOpen) * 100;

        // Расчет коэффициента профита
        float tpToSl = slPoints != 0 ? tpPoints / slPoints : 0;

        return "\r\n PriceOpen:" + String.format("%.4f", priceOpen) +
                ". \r\n SL:" + String.format("%.4f", stopLoss) +
                " (" + String.format("%.2f", slPoints) + " pts, " + String.format("%.2f", slPercent) + "%%)" +
                ". \r\n TP:" + String.format("%.4f", takeProfit) +
                " (" + String.format("%.2f", tpPoints) + " pts, " + String.format("%.2f", tpPercent) + "%%)" +
                ". \r\n Kprofit:" + String.format("%.2f", tpToSl);
    }


    private record Extremum(float value, int index) {
    }

    private record ExtremumResult(
            Extremum left,
            Extremum right,
            int trend
    ) {
    }

    private record FiboLevels(float fibo382, float fibo618, float rangeSize, float touchRegistrationGap, float levelBreakdownGap) {
    }

    private record LevelCheckResult(boolean isBroken, List<Marker> markers) {
    }

    private record TouchCountResult(int touches, List<Marker> markers) {
    }

    private record LevelAnalysisResult(
            boolean isBroken382,
            boolean isBroken618,
            int touches382,
            int touches618,
            List<Marker> markers382,
            List<Marker> markers618
    ) {
    }
}
