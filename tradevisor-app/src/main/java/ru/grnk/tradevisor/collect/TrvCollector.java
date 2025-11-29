package ru.grnk.tradevisor.collect;

import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;

import java.util.List;

public interface TrvCollector <T> {
    List<T> collect(Tickers tickers);
}
