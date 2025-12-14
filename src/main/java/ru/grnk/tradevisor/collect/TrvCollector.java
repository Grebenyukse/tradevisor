package ru.grnk.tradevisor.collect;

import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.util.List;

public interface TrvCollector <T> {
    List<T> collect(Tickers tickers);
}
