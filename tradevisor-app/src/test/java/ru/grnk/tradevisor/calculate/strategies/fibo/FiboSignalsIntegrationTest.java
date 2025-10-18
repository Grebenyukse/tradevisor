package ru.grnk.tradevisor.calculate.strategies.fibo;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.tinkoff.piapi.core.SignalService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FiboSignalsIntegrationTest {

    @Autowired
    private FiboSignals fiboSignals;

    @MockBean
    private MarketDataRepository marketDataRepository;

    @MockBean
    private SignalService signalService;

    private List<MarketData> generateCandles(float[] lows, float[] highs) {
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

    @Test
    void testTwoTouchesAtFibo318_ShouldGenerateLongSignal() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Simulate two touches at fibo318 level (~90.5)
        candles.get(10).setLow(90.5f);
        candles.get(13).setLow(90.5f);

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.LONG);
    }

    @Test
    void testThreeTouchesAtFibo318_ShouldGenerateLongSignal() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Simulate three touches at fibo318 level (~90.5)
        candles.get(10).setLow(90.5f);
        candles.get(13).setLow(90.5f);
        candles.get(16).setLow(90.5f);

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.LONG);
    }

    @Test
    void testTwoTouchesAtFibo618_ShouldGenerateLongSignal() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Simulate two touches at fibo618 level (~87.5)
        candles.get(12).setLow(87.5f);
        candles.get(15).setLow(87.5f);

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.LONG);
    }

    @Test
    void testThreeTouchesAtFibo618_ShouldGenerateLongSignal() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Simulate three touches at fibo618 level (~87.5)
        candles.get(12).setLow(87.5f);
        candles.get(15).setLow(87.5f);
        candles.get(18).setLow(87.5f);

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.LONG);
    }

    @Test
    void testBreakOfFibo318_NoSignalExpected() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Break below fibo318 level (~90.5)
        candles.get(14).setLow(89f); // пробой уровня

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.UNKNOWN);
    }

    @Test
    void testBreakOfFibo618_NoSignalExpected() {
        // Arrange
        float[] lows = {100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f, 82f, 81f};
        float[] highs = {102f, 101f, 100f, 99f, 98f, 97f, 96f, 95f, 94f, 93f, 92f, 91f, 90f, 89f, 88f, 87f, 86f, 85f, 84f, 83f};

        var candles = generateCandles(lows, highs);

        // Break below fibo618 level (~87.5)
        candles.get(16).setLow(86f); // пробой уровня

        // Act
        TrvCalculationResult result = fiboSignals.calculate(candles);

        // Assert
        assertThat(result.direction()).isEqualTo(TradingDirection.UNKNOWN);
    }
}
