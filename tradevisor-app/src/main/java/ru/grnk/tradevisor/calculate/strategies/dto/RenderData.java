package ru.grnk.tradevisor.calculate.strategies.dto;
import java.util.List;

public record RenderData(
        List<OhlcRecord> data,
        List<Integer> fiboXaxe,
        Float fibo382,
        Float fibo618,
        List<Marker> markers,
        Float priceOpen,
        Float stopLoss,
        Float takeProfit,
        Float infimum,
        Float supremum
) {
}