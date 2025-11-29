package ru.grnk.tradevisor.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.integration.testconfig.DotenvTestConfig;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.CREATED;
import static ru.grnk.tradevisor.testutils.TestUtils.*;

@Import(DotenvTestConfig.class)
public class PlotSignalIntegrationTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void additionalConfig(DynamicPropertyRegistry registry) {
        registry.add("app.integration.tinkoff.enabled", () -> "true");
        registry.add("app.integration.telegram.enabled", () -> "true");
        registry.add("app.integration.telegram.re-register", () -> "false");
        registry.add("app.integration.bybit.enabled", () -> "true");
        registry.add("app.integration.yahoofinance.enabled", () -> "true");
//        registry.add("app.integration.gigachat.enabled", () -> "true");
        registry.add("app.integration.deepseek.enabled", () -> "true");
//        registry.add("app.integration.proxyapi.enabled", () -> "true");
        registry.add("app.integration.cloudru.enabled", () -> "true");
//        registry.add("app.calculate.fibo", () -> "true");
        registry.add("app.calculate.bars_required_to_calculate_fibo", () -> 150);
        registry.add("app.notification.enabled", () -> "true");
        registry.add("app.collect.events.economic", () -> "true");
//        registry.add("app.collect.prices.finam", () -> "true");
//        registry.add("app.collect.prices.tinkoff", () -> "true");
//        registry.add("app.collect.prices.bybit", () -> "true");
//        registry.add("app.collect.prices.yahoofinance", () -> "true");
    }

    @Test
    void should_save_signal_fibo() {
        assertThat(signalsRepository.findUnpublishedSignals()).isEmpty();
        List<MarketData> candles = loadMarketDataFromCsv();
        marketDataRepository.batchInsertMarketData(candles);
        executeSqlScript("src/test/resources/sql/PublishSignalIntegrationTest/fill_signal.sql");
        await(() -> !signalsRepository.findUnpublishedSignals().isEmpty());
        assertThat(signalsRepository.findUnpublishedSignals().size()).isEqualTo(1);
        var signal = signalsRepository.findUnpublishedSignals().get(0);
        assertThat(signal.getDirection()).isEqualTo(TradingDirection.LONG.directionCode());
        assertThat(signal.getName()).isEqualTo("fibo");
        assertThat(signal.getStatus()).isEqualTo(CREATED.name());
        assertThat(df.format(signal.getPriceOpen())).isEqualTo(df.format(3.629f));
        assertThat(df.format(signal.getStopLoss())).isEqualTo(df.format(0f));
        assertThat(df.format(signal.getTakeProfit())).isEqualTo(df.format(11.742f));
        await(() -> false);
    }

    private List<MarketData> loadMarketDataFromCsv() {
        List<MarketData> marketDataList = new ArrayList<>();
        String filePath = "csv/market_data_202511242153.csv";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.readLine();
            String line;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXX");
            while ((line = reader.readLine()) != null) {
                String[] parts = line.replace("\"", "").split(",");
                MarketData marketData = new MarketData();
                marketData.setTickerCode(parts[0]); // ticker_code
                String dateTimeStr = parts[1]; // time
                OffsetDateTime time = OffsetDateTime.parse(dateTimeStr, formatter);
                marketData.setTime(time);
                marketData.setOpen(Float.parseFloat(parts[2]));
                marketData.setHigh(Float.parseFloat(parts[3]));
                marketData.setLow(Float.parseFloat(parts[4]));
                marketData.setClose(Float.parseFloat(parts[5]));
                marketDataList.add(marketData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return marketDataList;
    }
}
