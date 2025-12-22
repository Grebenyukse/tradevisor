package ru.grnk.tradevisor.trade;

import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.util.List;

public interface TradeClient {

    String provider();

    List<TrvOrder> getOrdersByTicker(String tickerCode);

    TrvPosition getAvgPositionByTicker(String tickerCode);

    boolean openPosition(Signals signal);

    void deleteOrders(String tickerCode);

}
