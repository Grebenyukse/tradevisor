package ru.grnk.tradevisor.collect.prices;

public interface PricesLoader {

    void loadPrices(String tickerUid);

    String getProvider();
}
