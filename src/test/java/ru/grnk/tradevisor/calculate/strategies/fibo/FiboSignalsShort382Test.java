package ru.grnk.tradevisor.calculate.strategies.fibo;

import org.junit.jupiter.api.Test;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static ru.grnk.tradevisor.testutils.TestUtils.generateCandles;
import static ru.grnk.tradevisor.testutils.TestUtils.reverse;

class FiboSignalsShort382Test {

    /**
     * 100% ┤------------------------------------------------------------
     *  100% |                   |
     *   93% |                 ||||||          ||    ||||          ||    |
     *   87% |                |||  |||        |||| ||| |||        |||| |||
     *   80% |               |||    |||      |||||||||  |||      |||||||||
     *   73% |             ||||      ||||||||||  |       ||||||||||  |
     *   67% |            |||          |  ||               |  ||
     *   60% |          ||||               |                   |
     *   53% |         |||
     *   47% |        |||
     *   40% |      ||||
     *   33% |     |||
     *   27% |   ||||
     *   20% |  |||
     *   13% | |||
     *    7% ||||
     *    0% ||
     *    0% └------------------------------------------------------------
     */
    @Test
    void getFiboSignals_382_short_2_touches() {
        // Arrange
        float[] lows = {
                // рост 0‑100%
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f, 90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,92.978f, // touch-2
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,92.978f, // touch-1
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f
        };
        float[] highs = {
                // рост
                82f,83f,84f,85f,86f,87f,88f,89f,90f,91f, 92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isPresent());
        var resultParams = result.get();
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        assertThat(resultParams.direction()).isEqualTo(TradingDirection.SHORT);
        assertThat(resultParams.lots()).isEqualTo(2);
        assertThat(df.format(resultParams.priceOpen())).isEqualTo(df.format(96.989f));
        assertThat(df.format(resultParams.stopLoss())).isEqualTo(df.format(101.00f));
        assertThat(df.format(resultParams.takeProfit())).isEqualTo(df.format(88.02201));
    }


    /**
     * task-register-gap - ok. недошли, но касание зарегистрировано
     * level-breakdown-gap - ok. пробили, но не слишком сильно, пробой уровня не состоялся. сигнал живет.
     */
    @Test
    public void levelBreakdownGap_taskRegistrationGap_levels_no_break_test() {
            // Arrange
            float[] lows = {
                    // рост 0‑100%
                    80f,81f,82f,83f,84f,85f,86f,87f,88f,89f, 90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                    // откат до 38.2% (≈ 87.64)
                    99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f - 0.20f), // touch-2 level breakdown ok.
                    // отскок
                    95f,96f,96f,97f,97f,96f,97f,98f,97f,97f,
                    // откат до 38.2% (≈ 87.64)
                    99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f + 0.62f), // touch-1 touch registration ok.
                    // отскок
                    95f,96f,96f,97f,97f,96f,97f,98f,97f,97f
            };
            float[] highs = {
                    // рост
                    82f,83f,84f,85f,86f,87f,88f,89f,90f,91f, 92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                    // откат
                    100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                    // отскок
                    96f,97f,98f,99f,99f,98f,97f,97f,98f,99f,
                    // откат
                    100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                    // отскок
                    96f,97f,98f,99f,99f,98f,97f,97f,98f,99f
            };
            reverse(lows);
            reverse(highs);
            var candles = generateCandles(lows, highs);
            Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
            assertTrue(result.isPresent());
            var resultParams = result.get();
            DecimalFormat df = new DecimalFormat("#.#####");
            df.setRoundingMode(RoundingMode.CEILING);
            assertThat(resultParams.direction()).isEqualTo(TradingDirection.SHORT);
            assertThat(resultParams.lots()).isEqualTo(2);
            assertThat(df.format(resultParams.priceOpen())).isEqualTo(df.format(96.989f));
            assertThat(df.format(resultParams.stopLoss())).isEqualTo(df.format(101.00f));
            assertThat(df.format(resultParams.takeProfit())).isEqualTo(df.format(88.02201));
    }

    @Test
    public void touchRegisterGap_failed_no_signal_test() {
        // Arrange
        float[] lows = {
                // рост 0‑100%
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f, 90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f - 0.20f), // touch-2 level breakdown ok.
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f + 0.64f), // touch-1 no touch. registration failed.
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f
        };
        float[] highs = {
                // рост
                82f,83f,84f,85f,86f,87f,88f,89f,90f,91f, 92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isEmpty());
    }

    @Test
    public void levelBreakDown_no_signal_test() {
        // Arrange
        float[] lows = {
                // рост 0‑100%
                80f,81f,82f,83f,84f,85f,86f,87f,88f,89f, 90f,91f,92f,93f,94f,95f,96f,97f,98f,99f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f - 0.22f), // touch-2 level breakdown. no signal.
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f,
                // откат до 38.2% (≈ 87.64)
                99f,98f,97f,96f,95f,94.7f,94.8f,94.9f,94.5f,(92.978f + 0.62f), // touch-1 touch registration ok.
                // отскок
                95f,96f,96f,97f,97f,96f,97f,98f,97f,97f
        };
        float[] highs = {
                // рост
                82f,83f,84f,85f,86f,87f,88f,89f,90f,91f, 92f,93f,94f,95f,96f,97f,98f,99f,100f,101f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f,
                // откат
                100f,100f,99f,98f,97f,96f,95f,95f,95f,95f,
                // отскок
                96f,97f,98f,99f,99f,98f,97f,97f,98f,99f
        };
        reverse(lows);
        reverse(highs);
        var candles = generateCandles(lows, highs);
        Optional<TrvCalculationResult> result = FiboSignalsProducer.getFiboSignals(candles, 2);
        assertTrue(result.isEmpty());
    }


}