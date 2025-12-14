package ru.grnk.tradevisor.common.properties;

public record TrvCalculateProperties(
        String delay,
        Boolean threeBarsGrowth,
        Boolean fibo,
        Boolean gap,
        Boolean alwaysBuy,
        Integer barsRequiredToCalculateFibo,
        Integer minTouchesCount,
        Integer batchSize
) {
}
