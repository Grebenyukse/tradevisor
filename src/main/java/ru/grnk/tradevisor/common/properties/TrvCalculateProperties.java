package ru.grnk.tradevisor.common.properties;

public record TrvCalculateProperties(
        String delay,
        Boolean threeBarsGrowth,
        Boolean fibo,
        Boolean gap,
        Boolean alwaysBuy,
        Integer barsRequiredToCalculateFibo,
        TrvCalcMinTouchesCount minTouchesCount,
        Integer batchSize,
        Boolean tenx,
        Integer barsRequiredToCalculateTenx,
        Integer tenxLookbackBars,
        Double  tenxGrowthFactor,
        Double tenxPriceMultiplier,
        Double tenxStopLossMultiplier
){
}