package ru.grnk.tradevisor.trade;

import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.trade.dto.TrvOrder;

import java.util.List;

public interface TradeClient {

    String provider();

    List<TrvOrder> getOrdersByTicker(String tickerCode);

    boolean openPosition(Signals signal);

    void deleteOrders(String tickerCode);

    boolean isPositionOpened(String tickerCode);

    void checkPositionStatus(String tickerCode);

}
