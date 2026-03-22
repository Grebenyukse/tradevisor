package ru.grnk.tradevisor.integration.strategies.tenx;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.integration.BaseIntegrationTest;
import ru.grnk.tradevisor.integration.testconfig.DotenvTestConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.CREATED;
import static ru.grnk.tradevisor.testutils.TestUtils.await;

@Import(DotenvTestConfig.class)
public class PlotTenxSignalIntegrationTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void additionalConfig(DynamicPropertyRegistry registry) {
        registry.add("app.integration.bybit.enabled", () -> "true");
        registry.add("app.integration.telegram.enabled", () -> "false");
        registry.add("app.integration.telegram.re-register", () -> "false");
        registry.add("app.calculate.tenx.bars_required", () -> 150);
        registry.add("app.calculate.tenx.enabled", () -> "true");
        registry.add("app.notification.enabled", () -> "true");
    }

    @Test
    void should_save_signal_tenx() {
        assertThat(signalsRepository.findUnpublishedSignals()).isEmpty();
        List<MarketData> candles = loadMarketDataCsv.loadMarketDataFromCsv();
        marketDataRepository.batchInsertMarketData(candles);
        executeSqlScript("src/test/resources/sql/PublishSignalIntegrationTest/fill_signal_tenx.sql");
        await(() -> !signalsRepository.findUnpublishedSignals().isEmpty());
        assertThat(signalsRepository.findUnpublishedSignals().size()).isEqualTo(1);
        var signal = signalsRepository.findUnpublishedSignals().get(0);
        assertThat(signal.getDirection()).isEqualTo(TradingDirection.LONG.directionCode());
        assertThat(signal.getName()).isEqualTo("tenx");
        assertThat(signal.getStatus()).isEqualTo(CREATED.name());
        assertThat(df.format(signal.getPriceOpen())).isEqualTo(df.format(3.629f));
        assertThat(df.format(signal.getStopLoss())).isEqualTo(df.format(0f));
        assertThat(df.format(signal.getTakeProfit())).isEqualTo(df.format(11.742f));
    }
}
