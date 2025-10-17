package ru.grnk.tradevisor.calculate.strategies.fibo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.*;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.*;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(value = "app.calculate.fibo")
public class FiboSignals implements IStrategy {

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return 100;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return new TrvCalculationResult(
                TradingDirection.UNKNOWN, null, null, null, null, List.of());
    }

    @Override
    public String getStrategyUniqueName() {
        return "fibo";
    }

    public static SignalResult getFiboSignals(List<OhlcRecord> data, boolean render) {
        List<OhlcRecord> tickerData = data.size() > 100 ? data.subList(0, 100) : data;
        if (tickerData.size() < 20) {
            return null;
        }
        int supremumBar = 0;
        double supremum = tickerData.get(0).high();
        for (int i = 1; i < tickerData.size(); i++) {
            if (tickerData.get(i).high() > supremum) {
                supremum = tickerData.get(i).high();
                supremumBar = i;
            }
        }
        int infimumBar = 0;
        double infimum = tickerData.get(0).low();
        for (int i = 1; i < tickerData.size(); i++) {
            if (tickerData.get(i).low() < infimum) {
                infimum = tickerData.get(i).low();
                infimumBar = i;
            }
        }
        if (supremumBar == infimumBar) {
            return null;
        }
        int leftEBar = Math.max(supremumBar, infimumBar);
        int rightEBar = Math.min(supremumBar, infimumBar);

        double leftExtremum, rightExtremum;
        int trend;

        if (leftEBar == supremumBar) {
            leftExtremum = supremum;
            rightExtremum = infimum;
            trend = -1;
        } else {
            leftExtremum = infimum;
            rightExtremum = supremum;
            trend = 1;
        }

        double rangeSize = supremum - infimum;

        double fibo382, fibo618;
        if (trend == 1) {
            fibo382 = rightExtremum - 0.382 * rangeSize;
            fibo618 = rightExtremum - 0.618 * rangeSize;
        } else {
            fibo382 = rightExtremum + 0.382 * rangeSize;
            fibo618 = rightExtremum + 0.618 * rangeSize;
        }

        boolean isbroken382 = false;
        boolean isbroken618 = false;
        int touches382 = 0;
        int touches681 = 0;
        double sigma = rangeSize * 0.03; // погрешность определения сигнала 3%
        double alpha = rangeSize * 0.01; // погрешность определения пробоя 1%

        List<Marker> markersTouples382 = new ArrayList<>();
        List<Marker> markersTouples618 = new ArrayList<>();

        // Проверяем, пробиты ли уровни Фибоначчи
        for (int i = 0; i < rightEBar; i++) {
            if (trend == 1) {
                if (tickerData.get(i).low() < fibo382 - alpha) {
                    isbroken382 = true;
                    markersTouples382.add(new Marker(i, tickerData.get(i).low(), "black"));
                }
                if (tickerData.get(i).low() < fibo618 - alpha) {
                    isbroken618 = true;
                    markersTouples618.add(new Marker(i, tickerData.get(i).low(), "black"));
                }
            }
            if (trend == -1) {
                if (tickerData.get(i).high() > fibo382 + alpha) {
                    isbroken382 = true;
                    markersTouples382.add(new Marker(i, tickerData.get(i).high(), "black"));
                }
                if (tickerData.get(i).high() > fibo618 + alpha) {
                    isbroken618 = true;
                    markersTouples618.add(new Marker(i, tickerData.get(i).high(), "black"));
                }
            }
        }

        // Подсчитываем касания
        int j = 0;
        while (j < rightEBar) {
            if (trend == 1) {
                if (tickerData.get(j).low() - fibo382 < sigma) {
                    touches382++;
                    j += 2;
                    markersTouples382.add(new Marker(j, fibo382, "black"));
                }
                if (tickerData.get(j).low() - fibo618 < sigma) {
                    touches681++;
                    j += 2;
                    markersTouples618.add(new Marker(j, fibo618, "black"));
                }
            }
            if (trend == -1) {
                if (fibo382 - tickerData.get(j).high() < sigma) {
                    touches382++;
                    j += 2;
                    markersTouples382.add(new Marker(j, fibo382, "black"));
                }
                if (fibo618 - tickerData.get(j).high() < sigma) {
                    touches681++;
                    j += 2;
                    markersTouples618.add(new Marker(j, fibo618, "black"));
                }
            }
            j++;
        }

        double stopLoss = trend == 1 ? infimum : supremum;
        double takeProfit = fibo618;
        List<Marker> markers = null;
        Double priceOpen = null;

        if ((!isbroken382) && (touches382 >= 2)) {
            markers = new ArrayList<>(markersTouples382);
            priceOpen = (stopLoss + fibo382) / 2;
            String description = "touches:" + touches382 + "." + getPositionInfo(priceOpen, takeProfit, stopLoss);
            return new SignalResult(
                    tickerData.get(0).ticker(),
                    tickerData.get(rightEBar).datetime(),
                    "Fibo touch 38.2",
                    trend,
                    touches382,
                    description
            );
        }

        if ((!isbroken618) && (touches681 >= 2)) {
            markers = new ArrayList<>(markersTouples618);
            priceOpen = fibo382;
            String description = "touches:" + touches681 + getPositionInfo(priceOpen, takeProfit, stopLoss);
            return new SignalResult(
                    tickerData.get(0).ticker(),
                    tickerData.get(rightEBar).datetime(),
                    "Fibo touch 61.8",
                    trend,
                    touches681,
                    description
            );
        }

        if (render) {
            if (((!isbroken382) && (touches382 >= 2)) || ((!isbroken618) && (touches681 >= 2))) {
                List<Integer> fiboXaxe = IntStream.rangeClosed(rightEBar, leftEBar)
                        .boxed()
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);

                return null; // В Java мы не можем вернуть кортеж, поэтому возвращаем null здесь
                // Для полной реализации render нужно создать отдельный метод
            }
        }

        return null;
    }

    private static String getPositionInfo(Double priceOpen, double takeProfit, double stopLoss) {
        double tpToSl = Math.abs((priceOpen - takeProfit) / (priceOpen - stopLoss));
        return "\r\n PriceOpen:" + String.format("%.4f", priceOpen) +
                ". \r\n SL:" + String.format("%.4f", stopLoss) +
                ". \r\n TP:" + String.format("%.4f", takeProfit) +
                ". \r\n Kprofit:" + String.format("%.2f", tpToSl);
    }
}
