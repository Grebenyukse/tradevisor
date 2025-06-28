package ru.grnk.tradevisor.collect.prices;

import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.Share;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Shares2TickerMapper {

    public static Tickers from(Share share) {
        return new Tickers()
                .setCurrency(share.getCurrency())
                .setDescription(share.getName())
                .setTicker(share.getTicker())
                .setExchange(share.getExchange())
                .setExpiration(null)
                .setGo(null)
                .setLot(share.getLot())
                .setUuid(share.getUid())
                .setFigi(share.getFigi())
                .setMarketType("акции")
                .setPrecision(1);

    }

    public static Tickers from(Future future) {
        return new Tickers()
                .setCurrency(future.getCurrency())
                .setDescription(future.getName())
                .setTicker(future.getTicker())
                .setExchange(future.getExchange())
                .setExpiration(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(future.getExpirationDate().getSeconds()),
                        ZoneId.systemDefault())
                )
                .setGo(null)
                .setLot(future.getLot())
                .setUuid(future.getUid())
                .setFigi(future.getFigi())
                .setMarketType("фьючерсы")
                .setPrecision(1);
    }
}
