package ru.grnk.tradevisor.calculate.strategies.tenx;

import org.junit.jupiter.api.Test;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.testutils.TestUtils.generateCandles;
import static ru.grnk.tradevisor.testutils.TestUtils.reverse;

class TenxSignalsShortTest {

    /**
     *  100% ┤------------------------------------------------------------
     *  100% ||
     *   93% | |
     *   87% |  ||
     *   80% |    |
     *   73% |     |
     *   67% |      |
     *   60% |       ||
     *   53% |         |
     *   47% |          |
     *   40% |           ||
     *   33% |             |            ||||                ||||
     *   27% |              |          |||| |              |||| |
     *   20% |               |        ||||  ||    ||      ||||  ||    ||
     *   13% |                ||    ||       |||||||||  ||       |||||||||
     *    7% |                  | ||               |  ||               |
     *    0% |                   ||                   |
     *    0% └------------------------------------------------------------
     */
    @Test
    void getFiboSignals_382_long_2_touches() {
        float[] lows = {
                18.5f,
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.1f, // infimum
                0.1f, 1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.02f, 4.0f, 3.0f,
                3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f, 0.1f,
                1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.0f, 4.0f,
                3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f
        };

        float[] highs = {
                19.0f, // supremum
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.1f,
                1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, 7.258f, // touch-1
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f, 1.0f, // rollback
                1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, 7.2580f, // touch - 2
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f // rollback
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        TrvCalculationResult result = TenxSignalsProducer.getTenxSignals(
                candles,
                highs.length -1, 10, 15, 10);
        assertThat(result.direction()).isEqualTo(TradingDirection.SHORT);
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        assertThat(result.lots()).isEqualTo(1);
        assertThat(df.format(result.priceOpen())).isEqualTo(df.format(1.5f));
        assertThat(df.format(result.stopLoss())).isEqualTo(df.format(72.58f));
        assertThat(df.format(result.takeProfit())).isEqualTo(df.format(0.1f));
    }



}