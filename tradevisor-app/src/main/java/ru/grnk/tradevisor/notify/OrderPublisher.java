package ru.grnk.tradevisor.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.telegram.TelegramMessageService;
import ru.grnk.tradevisor.notify.plot.PlotService;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderPublisher {

    private final PlotService plotService;
    private final TelegramMessageService telegramMessageService;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    private final static Map<String, String> PROVIDER_TO_BASE_URL = Map.of("finam", "https://trading.finam.ru/profile/");

    @Transactional
    public void publishOrder(Signals signal) {
        String image = plotService.saveCandlestickChartToFile(signal, true);
        if (image == null ) return;
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        telegramMessageService.sendMessage(image, getTitle(signal, ticker), getText(signal, ticker), signal.getId(),
                tradevisorProperties.integration().telegram().supergroup().ordersThreadId());
    }

    private static String getTitle(Signals signal, Tickers ticker) {
        return String.join(". ",
                "Ордер опубликован",ticker.getTicker(), ticker.getExchange(), ticker.getProvider(),
                TradingDirection.from(signal.getDirection()).name()
        );
    }

    private static String getText(Signals signal, Tickers ticker) {
        return TradingDirection.from(signal.getDirection()).name()
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