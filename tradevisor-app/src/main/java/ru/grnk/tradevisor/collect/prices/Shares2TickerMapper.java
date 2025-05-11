package ru.grnk.tradevisor.collect.prices;

import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.contract.v1.Share;

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
}
