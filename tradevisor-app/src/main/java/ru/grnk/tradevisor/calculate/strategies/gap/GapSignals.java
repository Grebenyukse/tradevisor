package ru.grnk.tradevisor.calculate.strategies.gap;

import ru.grnk.tradevisor.calculate.strategies.dto.Marker;
import ru.grnk.tradevisor.calculate.strategies.dto.OhlcRecord;
import ru.grnk.tradevisor.calculate.strategies.dto.RenderData;
import ru.grnk.tradevisor.calculate.strategies.dto.SignalResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GapSignals {

    public static Optional<RenderData> getGapSignals(List<OhlcRecord> data, boolean render) {
        if (data.size() < 20) {
            return Optional.empty();
        }

        List<OhlcRecord> OhlcRecord = data.subList(0, Math.min(100, data.size()));
        System.out.println("check for gap in " + OhlcRecord.get(0).ticker());

        double rangeSize = OhlcRecord.stream().mapToDouble(OhlcRecord::high).max().orElse(0) -
                OhlcRecord.stream().mapToDouble(OhlcRecord::low).min().orElse(0);
        double minGapSize = rangeSize * 0.1;

        Double supremum = null;
        Double infimum = null;
        Integer gapBar = null;
        int trend = 0;

        List<Marker> markersTuplesInfimum = new ArrayList<>();
        List<Marker> markersTuplesSupremum = new ArrayList<>();

        for (int i = 0; i < OhlcRecord.size() - 1; i++) {
            double gap = OhlcRecord.get(i).open() - OhlcRecord.get(i + 1).close();
            if (Math.abs(gap) > minGapSize) {
                if (gap > 0) {
                    supremum = OhlcRecord.get(i).open();
                    infimum = OhlcRecord.get(i + 1).close();
                    trend = 1;
                } else {
                    supremum = OhlcRecord.get(i + 1).close();
                    infimum = OhlcRecord.get(i).open();
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
                if (OhlcRecord.get(j).high() - supremum > omega) {
                    gapIsBroken = true;
                    markersTuplesSupremum.add(new Marker(j, OhlcRecord.get(j).high(), "black"));
                }
            }
            if (trend == 1) {
                if (infimum - OhlcRecord.get(j).low() > omega) {
                    gapIsBroken = true;
                    markersTuplesInfimum.add(new Marker(j, OhlcRecord.get(j).low(), "black"));
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
                if (supremum - OhlcRecord.get(k).high() < sigma) {
                    supremumTouches++;
                    k += 2;
                    markersTuplesSupremum.add(new Marker(k, OhlcRecord.get(k).high(), "black"));
                }
            }
            if (trend == 1) {
                if (OhlcRecord.get(k).low() - infimum < sigma) {
                    infimumTouches++;
                    k += 2;
                    markersTuplesInfimum.add(new Marker(k, OhlcRecord.get(k).low(), "black"));
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
                    OhlcRecord.get(0).ticker(),
                    OhlcRecord.get(gapBar).datetime(),
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

            return Optional.of(new RenderData(
                    null, null, null, null, null, null, null, null, null, null
            ));
        }

        return Optional.empty();
    }

    private static String getPositionInfo(Double priceOpen, Double takeProfit, Double stopLoss) {
        // Placeholder implementation
        return "";
    }
}

