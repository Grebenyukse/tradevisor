package ru.grnk.tradevisor.common.properties;

public record TrvCalculateProperties(
        String delay,
        Integer batchSize,
        GapProperties gap,
        FiboProperties fibo,
        TenxProperties tenx
){
    public record GapProperties(Boolean enabled, Integer barsRequired) {};
    public record FiboProperties(Boolean enabled,
                                        Integer barsRequired,
                                        TrvCalcMinTouchesCount minTouchesCount
                                        ) {};
    public record TenxProperties(Boolean enabled,
                                 Integer barsRequired,
                                 Integer lookBackBars,
                                 Double growthFactor,
                                 Double priceMultiplier,
                                 Double stopLossMultiplier) {};
}