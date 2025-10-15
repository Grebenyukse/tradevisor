package ru.grnk.tradevisor.calculate.strategies.gap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.*;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(value = "app.calculate.gap")
public class GapSignals implements IStrategy {

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return 100;
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        return new TrvCalculationResult(
                TradingDirection.UNKNOWN, null, null, null, null);
    }

    @Override
    public String getStrategyUniqueName() {
        return "gap";
    }

    public static Optional<RenderData> getGapSignals(List<OhlcRecord> data, boolean render) {
        if (data.size() < 20) {
            return Optional.empty();
        }

        List<OhlcRecord> ohlcRecord = data.subList(0, Math.min(100, data.size()));
        System.out.println("check for gap in " + ohlcRecord.get(0).ticker());

        double rangeSize = ohlcRecord.stream().mapToDouble(OhlcRecord::high).max().orElse(0) -
                ohlcRecord.stream().mapToDouble(OhlcRecord::low).min().orElse(0);
        double minGapSize = rangeSize * 0.1;

        Double supremum = null;
        Double infimum = null;
        Integer gapBar = null;
        int trend = 0;

        List<Marker> markersTuplesInfimum = new ArrayList<>();
        List<Marker> markersTuplesSupremum = new ArrayList<>();

        for (int i = 0; i < ohlcRecord.size() - 1; i++) {
            double gap = ohlcRecord.get(i).open() - ohlcRecord.get(i + 1).close();
            if (Math.abs(gap) > minGapSize) {
                if (gap > 0) {
                    supremum = ohlcRecord.get(i).open();
                    infimum = ohlcRecord.get(i + 1).close();
                    trend = 1;
                } else {
                    supremum = ohlcRecord.get(i + 1).close();
                    infimum = ohlcRecord.get(i).open();
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
                if (ohlcRecord.get(j).high() - supremum > omega) {
                    gapIsBroken = true;
                    markersTuplesSupremum.add(new Marker(j, ohlcRecord.get(j).high(), "black"));
                }
            }
            if (trend == 1) {
                if (infimum - ohlcRecord.get(j).low() > omega) {
                    gapIsBroken = true;
                    markersTuplesInfimum.add(new Marker(j, ohlcRecord.get(j).low(), "black"));
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
                if (supremum - ohlcRecord.get(k).high() < sigma) {
                    supremumTouches++;
                    k += 2;
                    markersTuplesSupremum.add(new Marker(k, ohlcRecord.get(k).high(), "black"));
                }
            }
            if (trend == 1) {
                if (ohlcRecord.get(k).low() - infimum < sigma) {
                    infimumTouches++;
                    k += 2;
                    markersTuplesInfimum.add(new Marker(k, ohlcRecord.get(k).low(), "black"));
                }
            }
            k++;
        }

        if (supremumTouches > 1 || infimumTouches > 1) {
            Double takeProfit = null;
            Double stopLoss = null;
            Double priceOpen = null;
            List<Marker> markers = null;

            if (supremumTouches > 1) {
                takeProfit = supremum;
                stopLoss = infimum - minGapSize;
                priceOpen = infimum;
                markers = markersTuplesSupremum;
            }

            if (infimumTouches > 1) {
                takeProfit = infimum;
                stopLoss = supremum + minGapSize;
                priceOpen = supremum;
                markers = markersTuplesInfimum;
            }

            String positionInfo = getPositionInfo(priceOpen, takeProfit, stopLoss);
            String description = (supremumTouches > 1 ? "touches:" + supremumTouches : "touches:" + infimumTouches) +
                    "." + positionInfo;

            SignalResult resultDf = new SignalResult(
                    ohlcRecord.get(0).ticker(),
                    ohlcRecord.get(gapBar).datetime(),
                    "Gap touch",
                    trend,
                    supremumTouches > 1 ? supremumTouches : infimumTouches,
                    description
            );

            if (render) {
                List<Integer> fiboXaxe = List.of(gapBar, gapBar + 1);
                Double fibo382 = infimum;
                Double fibo618 = supremum;

                return Optional.of(new RenderData(
                        null, // data not implemented
                        fiboXaxe,
                        fibo382,
                        fibo618,
                        null, // markers SignalResult not implemented
                        priceOpen,
                        stopLoss,
                        takeProfit,
                        infimum,
                        supremum
                ));
            }

            return Optional.empty();
        }

        return Optional.empty();
    }

    private static String getPositionInfo(Double priceOpen, Double takeProfit, Double stopLoss) {
        // Placeholder implementation
        return "";
    }
}

