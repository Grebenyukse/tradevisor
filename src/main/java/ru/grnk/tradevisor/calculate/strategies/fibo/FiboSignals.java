package ru.grnk.tradevisor.calculate.strategies.fibo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.util.List;

import static java.util.Optional.ofNullable;
import static ru.grnk.tradevisor.calculate.strategies.fibo.FiboSignalsProducer.getFiboSignals;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnProperty(value = "app.calculate.fibo.enabled")
public class FiboSignals implements IStrategy {
    private final TradevisorProperties tradevisorProperties;
    private final TickersRepository tickersRepository;

    @Override
    public Integer barsRequiredToCalcStrategy() {
        return tradevisorProperties.calculate().fibo().barsRequired();
    }

    @Override
    public TrvCalculationResult calculate(List<MarketData> candles) {
        var defaultNoSignal = TrvCalculationResult.builder()
                .direction(TradingDirection.UNKNOWN)
                .build();
        var firstCandle = candles.stream().findFirst().orElse(null);
        if (firstCandle == null) {
            log.info("first candle is null");
            return defaultNoSignal;
        }
        var tradeTicker = ofNullable(tickersRepository.findTradeTickerByTickerCode(firstCandle.getTickerCode()))
                .orElseGet(() -> tickersRepository.getTickerByTickerCode(firstCandle.getTickerCode()));
        var touchesRequired = getTouchesByTickerProvider(tradeTicker.getProvider());
        return getFiboSignals(candles, touchesRequired).orElse(defaultNoSignal);
    }

    private int getTouchesByTickerProvider(String provider) {
        return switch (provider) {
            case "bybit" -> tradevisorProperties.calculate().fibo().minTouchesCount().crypto();
            case "finam" -> tradevisorProperties.calculate().fibo().minTouchesCount().rus();
            case "tinkoff" -> tradevisorProperties.calculate().fibo().minTouchesCount().world();
            default -> {
                log.error("unknown provider: {}",provider);
                yield 0;
            }
        };
    }

    @Override
    public String getStrategyUniqueName() {
        return "fibo";
    }


}
