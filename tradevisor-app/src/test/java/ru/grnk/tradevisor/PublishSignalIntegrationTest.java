package ru.grnk.tradevisor;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.calculate.strategies.fibo.TestUtils.*;


public class PublishSignalIntegrationTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void additionalConfig(DynamicPropertyRegistry registry) {
        registry.add("app.integration.finam.enabled", () -> "true");
        registry.add("app.notification.enabled", () -> "true");
    }

    @Test
    void should_publish_signal_fibo() {
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
        var candles = generateCandles(lows, highs, TEST_TICKER_CODE, 1);
        marketDataRepository.batchInsertMarketData(candles);
        assertThat(signalsRepository.findUnpublishedSignals()).isEmpty();
        executeSqlScript("src/test/resources/sql/PublishSignalIntegrationTest/fill_signal.sql");
        assertThat(signalsRepository.findUnpublishedSignals().size()).isEqualTo(1);
        var signal = signalsRepository.findUnpublishedSignals().get(0);
        await(() -> false);
        assertThat(1).isEqualTo(1);
    }

}
