package ru.grnk.tradevisor.calculate.strategies.fibo;

import org.junit.jupiter.api.Test;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.grnk.tradevisor.testutils.TestUtils.generateCandles;
import static ru.grnk.tradevisor.testutils.TestUtils.reverse;

class FiboSignalsLong382Test {

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
                0.0f, // infimum
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
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isPresent());
        var resultParams = result.get();
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        assertThat(resultParams.direction()).isEqualTo(TradingDirection.LONG);
        assertThat(resultParams.lots()).isEqualTo(2);
        assertThat(df.format(resultParams.priceOpen())).isEqualTo(df.format(3.629f));
        assertThat(df.format(resultParams.stopLoss())).isEqualTo(df.format(0.00f));
        assertThat(df.format(resultParams.takeProfit())).isEqualTo(df.format(11.742f));
    }

    /**
     * первый раз не дошли до уровня но зазор меньше чем touchRegistrationGap
     * второй подход прошли дальше уровня но пробой меньше чем levelBreakdownGap
     */
    @Test
    public void levelBreakdownGap_touchRegistrationGap_level_no_break_test() {
        float[] lows = {
                18.5f,
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.0f, // infimum
                0.1f, 1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.02f, 4.0f, 3.0f,
                3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f, 0.1f,
                1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.0f, 4.0f,
                3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f
        };

        float[] highs = {
                19.0f, // supremum
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.1f,
                1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f+0.18f), // touch-2 levelBreakdownGap ok
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f, 1.0f, // rollback
                1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f-0.56f), // touch - 1 touchRegistrationGap - ok
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f // rollback
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isPresent());
        var resultParams = result.get();
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        assertThat(resultParams.direction()).isEqualTo(TradingDirection.LONG);
        assertThat(resultParams.lots()).isEqualTo(2);
        assertThat(df.format(resultParams.priceOpen())).isEqualTo(df.format(3.629f));
        assertThat(df.format(resultParams.stopLoss())).isEqualTo(df.format(0.00f));
        assertThat(df.format(resultParams.takeProfit())).isEqualTo(df.format(11.742f));
    }


    /**
     * первый раз не дошли до уровня. зазор больше чем чем touchRegistrationGap. Касание не засчитано
     */
    @Test
    public void touchRegistrationGap_level_do_break_test() {
        float[] lows = {
                18.5f,
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.0f, // infimum
                0.1f, 1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.02f, 4.0f, 3.0f,
                3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f, 0.1f,
                1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.0f, 4.0f,
                3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f
        };

        float[] highs = {
                19.0f, // supremum
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.1f,
                1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f+0.18f), // touch-2 levelBreakdownGap ok
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f, 1.0f, // rollback
                1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f-0.59f), // touch not registered touchRegistrationGap - failed.
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f // rollback
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isEmpty());
    }


    /**
     * второй раз пробили уровень на зазор больше чем альфа. уровень пробит сигнала нет.
     */
    @Test
    public void levelBreakdownGap_level_do_break_test() {
        float[] lows = {
                18.5f,
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.0f, // infimum
                0.1f, 1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.02f, 4.0f, 3.0f,
                3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f, 0.1f,
                1.0f, 2.0f, 3.0f, 4.0f, 4.3f, 4.2f, 4.1f, 4.5f, 6.0f, 4.0f,
                3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 2.0f, 1.0f, 2.0f, 2.0f
        };

        float[] highs = {
                19.0f, // supremum
                18.0f, 17.0f, 16.0f, 15.0f, 14.0f, 13.0f, 12.0f, 11.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f,
                0.1f,
                1.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f+0.20f), // touch-2 levelBreakdownGap failed. level is broken
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f, 1.0f, // rollback
                1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 6.0f, (7.258f-0.56f), // touch 1. registered touchRegistrationGap - ok.
                6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 2.0f, 3.0f, 4.0f, 4.0f, 3.0f, 2.0f // rollback
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isEmpty());
    }


}