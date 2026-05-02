package ru.grnk.tradevisor.calculate.strategies.channel;

import lombok.extern.slf4j.Slf4j;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ConvergingChannelStrategyProducer {

    private static final int BREAK_LINE_PERIOD = 3;

    public static TrvCalculationResult calculate(List<MarketData> candles, int barsRequired) {
        if (candles.size() < barsRequired) {
            return noSignal();
        }
        var recentCandles = candles.subList(BREAK_LINE_PERIOD, barsRequired);
        return analyze(recentCandles);
    }

    public static TrvCalculationResult analyze(List<MarketData> history) {
        int n = history.size();

        List<Point> highPoints = new ArrayList<>();
        List<Point> lowPoints = new ArrayList<>();
        int window = 3;

        // 1. Поиск экстремумов с учетом обратной индексации
        // Идем по массиву и ищем пики
        for (int i = window; i < n - window; i++) {
            MarketData curr = history.get(i);
            boolean isPeak = true, isTrough = true;

            for (int j = 1; j <= window; j++) {
                if (curr.getHigh() < history.get(i - j).getHigh() || curr.getHigh() < history.get(i + j).getHigh()) isPeak = false;
                if (curr.getLow() > history.get(i - j).getLow() || curr.getLow() > history.get(i + j).getLow()) isTrough = false;
            }

            // Для регрессии время X должно расти.
            // Превращаем индекс 0 в X = N, а индекс N в X = 0.
            double x = n - i;
            if (isPeak) highPoints.add(new Point(x, curr.getHigh()));
            if (isTrough) lowPoints.add(new Point(x, curr.getLow()));
        }

        // 2. Регрессия
        double[] regHigh = calculateRegression(highPoints);
        double[] regLow = calculateRegression(lowPoints);

        // 3. Расчет уровней для текущей свечи (индекс 0, значит X = n)
        double currentX = n;
        double resistance = regHigh[0] * currentX + regHigh[1];
        double support = regLow[0] * currentX + regLow[1];

        // 4. Проверка схождения (сравниваем начало истории и сейчас)
        double startX = 0;
        double distStart = (regHigh[0] * startX + regHigh[1]) - (regLow[0] * startX + regLow[1]);
        double distEnd = resistance - support;

        if (distEnd < distStart) {
            log.info("Статус: СХОДИТСЯ (Начало: {}, Сейчас: {})%n", distStart, distEnd);
            return TrvCalculationResult.builder()
                    .direction(TradingDirection.LONG)
                    .priceOpen((float) support)
                    .stopLoss((float) support / 2)
                    .takeProfit((float) resistance)
                    .lots(1)
                    .lines(List.of())
                    .description("convergence")
                    .build();
        } else {
           return noSignal();
        }
    }

    private static double[] calculateRegression(List<Point> points) {
        int n = points.size();
        if (n < 2) return new double[]{0, 0};
        double sX = 0, sY = 0, sXX = 0, sXY = 0;
        for (Point p : points) {
            sX += p.x;
            sY += p.y;
            sXX += p.x * p.x;
            sXY += p.x * p.y;
        }
        double k = (n * sXY - sX * sY) / (n * sXX - sX * sX);
        double b = (sY - k * sX) / n;
        return new double[]{k, b};
    }

    static class Point {
        double x, y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static TrvCalculationResult noSignal() {
        return TrvCalculationResult.builder()
                .direction(TradingDirection.UNKNOWN)
                .lines(Collections.emptyList())
                .description("No converging channel signal detected")
                .build();
    }

}
