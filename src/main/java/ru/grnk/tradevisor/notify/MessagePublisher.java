package ru.grnk.tradevisor.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.telegram.TelegramMessageService;
import ru.grnk.tradevisor.notify.plot.PlotService;

import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final SignalsRepository signalsRepository;
    private final PlotService plotService;
    private final TelegramMessageService telegramMessageService;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    private final static Map<String, String> PROVIDER_TO_BASE_URL = Map.of("finam", "https://trading.finam.ru/profile/");

    @Transactional
    public void publishMessage(Signals signal) {
        String image = plotService.saveCandlestickChartToFile(signal, true);
        if (image == null ) return;
        Tickers ticker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        telegramMessageService.sendMessage(image, getTitle(signal, ticker), getText(signal, ticker), signal.getId(), getThreadId(ticker));
        signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.PUBLISHED);
    }

    private int getThreadId(Tickers ticker) {
        int threadId = 0;
        String lowerExchange = ofNullable(ticker.getExchange()).map(String::toLowerCase).orElse("");
        if (Objects.equals(ticker.getProvider(), "bybit")) {
            threadId = tradevisorProperties.integration().telegram().supergroup().cryptoThreadId();
        } else if (lowerExchange.contains("moex") ||
                lowerExchange.contains("spb_ru") ||
                lowerExchange.contains("misx")
        ) {
            threadId = tradevisorProperties.integration().telegram().supergroup().rusThreadId();
        } else {
            threadId = tradevisorProperties.integration().telegram().supergroup().worldThreadId();
        }
        return threadId;
    }

    private static String getTitle(Signals signal, Tickers ticker) {
        return String.join(". ",
                ticker.getTicker(), ticker.getExchange(), ticker.getProvider(),
                TradingDirection.from(signal.getDirection()).name()
        );
    }

    private static String getText(Signals signal, Tickers ticker) {
        return TradingDirection.from(signal.getDirection()).name()
                + ". \n Ticker: " + ticker.getDescription()
                + ". \n Ticker Url: " + getTickerBaseUrlForProvider(ticker.getProvider()) + ticker.getTickerCode()
                + ". \n PriceOpen: " + signal.getPriceOpen()
                + ". \n TakeProfit: " + signal.getTakeProfit()
                + ". \n StopLoss: " + signal.getStopLoss()
                + ". \n ProducedAt: " + signal.getCreatedAt();
    }

    private static String getTickerBaseUrlForProvider(String provider) {
        return PROVIDER_TO_BASE_URL.get(provider);
    }
}