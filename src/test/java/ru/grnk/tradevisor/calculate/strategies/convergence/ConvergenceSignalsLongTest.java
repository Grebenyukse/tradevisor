package ru.grnk.tradevisor.calculate.strategies.convergence;

import org.junit.jupiter.api.Test;
import ru.grnk.tradevisor.calculate.strategies.channel.ConvergingChannelStrategyProducer;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.grnk.tradevisor.testutils.GenCandlesUtils.readMarketDataFromFile;

class ConvergenceSignalsLongTest {

    @Test
    void get_convergence_false_breakout_long_signal() {
        var path = "C:\\Users\\grebe\\IdeaProjects\\tradevisor\\src\\test\\java\\ru\\grnk\\tradevisor\\testutils\\bars.txt";
        List<MarketData> candles = readMarketDataFromFile(path, "TEST@NASDAQ");
        TrvCalculationResult result = ConvergingChannelStrategyProducer.calculate(candles, candles.size() -1 );
        assertThat(result.direction()).isEqualTo(TradingDirection.LONG);
    }
}
