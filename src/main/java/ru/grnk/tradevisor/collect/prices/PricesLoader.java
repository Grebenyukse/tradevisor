package ru.grnk.tradevisor.collect.prices;

import ru.grnk.tradevisor.common.repository.entity.Tickers;

public interface PricesLoader {

    void loadPrices(Tickers ticker);

    String getProvider();

    void initTickers();

    int loadOrder();

    float getBidForTicker(String tickerCode);
}
