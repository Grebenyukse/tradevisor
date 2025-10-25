package ru.grnk.tradevisor.calculate.strategies.fibo;

import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.time.OffsetDateTime;
import java.util.List;

public class TestUtils {

    public static List<MarketData> generateCandles(float[] lows, float[] highs) {
        var list = new java.util.ArrayList<MarketData>();

        for (int i = 0; i < lows.length; i++) {
            list.add(new MarketData()
                    .setId(i)
                    .setTickerCode("AAPL@NASDAQ")
                    .setLow(lows[i])
                    .setHigh(highs[i])
                    .setTime(OffsetDateTime.now().minusDays(lows.length - i)));
        }
        return list;
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
}
