package ru.grnk.tradevisor.collect.utils;

import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.Share;

public class Shares2TickerMapper {

    public static Tickers from(Share share) {
        return Tickers.builder()
                .currency(share.getCurrency())
                .description(share.getName())
                .ticker(share.getTicker())
                .exchange(share.getExchange())
                .tickerCode(share.getUid())
                .build();

    }

    public static Tickers from(Future future) {
        return Tickers.builder()
                .currency(future.getCurrency())
                .description(future.getName())
                .ticker(future.getTicker())
                .exchange(future.getExchange())
                .tickerCode(future.getUid())
                .build();
    }
}
