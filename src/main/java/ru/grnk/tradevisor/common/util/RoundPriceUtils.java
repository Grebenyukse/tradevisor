package ru.grnk.tradevisor.common.util;

import org.jetbrains.annotations.NotNull;
import ru.grnk.tradevisor.integration.finam.dto.Money;
import ru.tinkoff.piapi.contract.v1.MoneyValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.tinkoff.piapi.core.utils.MapperUtils.mapUnitsAndNanos;

public class RoundPriceUtils {

    @NotNull
    public static BigDecimal roundPrice(Float price, BigDecimal minPriceIncrement, int direction) {
        return direction > 0
                ? roundDownPrice(BigDecimal.valueOf((double) price), minPriceIncrement)
                : roundUpPrice(BigDecimal.valueOf((double) price), minPriceIncrement);
    }

    public static BigDecimal roundUpPrice(BigDecimal price, BigDecimal minPriceIncrement) {
        return price.divide(minPriceIncrement, 0, RoundingMode.UP)
                .multiply(minPriceIncrement);
    }

    public static BigDecimal roundDownPrice(BigDecimal price, BigDecimal minPriceIncrement) {
        return price.divide(minPriceIncrement, 0, RoundingMode.DOWN)
                .multiply(minPriceIncrement);
    }

    public static BigDecimal moneyToBigDecimal(MoneyValue money) {
        return mapUnitsAndNanos(money.getUnits(), money.getNano());
    }

    public static BigDecimal moneyToBigDecimal(Money money) {
        return mapUnitsAndNanos(money.units(), money.nanos());
    }

    public static BigDecimal moneyToBigDecimal(com.google.type.Money money) {
        return mapUnitsAndNanos(money.getUnits(), money.getNanos());
    }
}
