package ru.grnk.tradevisor.testutils;

import org.awaitility.Awaitility;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

public class TestUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(40000);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    public static List<MarketData> generateCandles(float[] lows, float[] highs) {
        return generateCandles(lows, highs, "AAPL@NASDAQ", 24);
    }



    public static List<MarketData> generateCandles(float[] lows, float[] highs, String tickerCode, int hoursInterval) {
        var list = new java.util.ArrayList<MarketData>();

        for (int i = 0; i < lows.length; i++) {
            list.add(new MarketData()
                    .setTickerCode(tickerCode)
                    .setLow(lows[i])
                    .setOpen(getRandFloat(lows[i], highs[i]))
                    .setClose(getRandFloat(lows[i], highs[i]))
                    .setHigh(highs[i])
                    .setTime(OffsetDateTime.now().minusHours((long) i * hoursInterval)));
        }
        return list;
    }

    public static float getRandFloat(float from, float to) {
        return from + (float) Math.random() * (to - from);
    }

    public static void reverse(float[] array) {
        if (array == null) {
            return;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            float temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
    }

    public static void upsideDown(float[] array) {
        if (array == null) {
            return;
        }
        float maxVal = 0f;
        for (var v : array) {
            if (maxVal < v) maxVal = v;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = maxVal - array[i];
        }
    }

    public static void await(Supplier<Boolean> conditionSupplier) {
        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .until(conditionSupplier::get);
    }
}
