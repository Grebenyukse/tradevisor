package ru.grnk.tradevisor.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

    public static float round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}
