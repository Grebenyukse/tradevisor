package ru.grnk.tradevisor.integration.strategies.tenx;


import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.integration.BaseIntegrationTest;
import ru.grnk.tradevisor.integration.testconfig.DotenvTestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.CREATED;
import static ru.grnk.tradevisor.testutils.TestUtils.*;

@Import(DotenvTestConfig.class)
public class SaveTenxSignalIntegrationTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void additionalConfig(DynamicPropertyRegistry registry) {
        registry.add("app.integration.bybit.enabled", () -> "true");
        registry.add("app.calculate.tenx.enabled", () -> "true");
        registry.add("app.calculate.tenx.bars-required", () -> 60);
        registry.add("app.notification.enabled", () -> "true");
    }

    @Test
    void should_save_signal_fibo() {
        assertThat(signalsRepository.findUnpublishedSignals()).isEmpty();
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
        var candles = generateCandles(lows, highs, TEST_TICKER_CODE, 1);
        marketDataRepository.batchInsertMarketData(candles);
        await(() -> !signalsRepository.findUnpublishedSignals().isEmpty());
        assertThat(signalsRepository.findUnpublishedSignals().size()).isEqualTo(1);
        var signal = signalsRepository.findUnpublishedSignals().get(0);
        assertThat(signal.getDirection()).isEqualTo(TradingDirection.SHORT.directionCode());
        assertThat(signal.getName()).isEqualTo("tenx");
        assertThat(signal.getStatus()).isEqualTo(CREATED.name());
        assertThat(df.format(signal.getPriceOpen())).isEqualTo(df.format(1.5f));
        assertThat(df.format(signal.getStopLoss())).isEqualTo(df.format(72.58f));
        assertThat(df.format(signal.getTakeProfit())).isEqualTo(df.format(0.1f));
    }
}
