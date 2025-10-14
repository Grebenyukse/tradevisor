package ru.grnk.tradevisor.calculate.strategies.dto;
import java.util.List;

public record RenderData(
        List<OhlcRecord> data,
        List<Integer> fiboXaxe,
        double fibo382,
        double fibo618,
        List<Marker> markers,
        Double priceOpen,
        double stopLoss,
        double takeProfit,
        double infimum,
        double supremum
) {
}