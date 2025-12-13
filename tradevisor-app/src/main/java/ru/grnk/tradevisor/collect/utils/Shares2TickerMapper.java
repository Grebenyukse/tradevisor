package ru.grnk.tradevisor.collect.utils;

import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.Share;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Shares2TickerMapper {

    public static Tickers from(Share share) {
        return Tickers.builder()
                .currency(share.getCurrency())
                .description(share.getName())
                .ticker(share.getTicker())
                .exchange(share.getExchange())
                .expiration(null)
                .go(null)
                .lot(share.getLot())
                .tickerCode(share.getUid())
                .figi(share.getFigi())
                .marketType("акции")
                .precision(1)
                .build();

    }

    public static Tickers from(Future future) {
        return Tickers.builder()
                .currency(future.getCurrency())
                .description(future.getName())
                .ticker(future.getTicker())
                .exchange(future.getExchange())
                .expiration(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(future.getExpirationDate().getSeconds()),
                        ZoneId.systemDefault())
                )
                .go(null)
                .lot(future.getLot())
                .tickerCode(future.getUid())
                .figi(future.getFigi())
                .marketType("фьючерсы")
                .precision(1)
                .build();
    }
}
