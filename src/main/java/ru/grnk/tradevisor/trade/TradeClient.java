package ru.grnk.tradevisor.trade;

import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.util.List;

public interface TradeClient {

    String provider();

    Float getBalance();

    Float getFreeMargin();

    String findTickerForSpot(String tickerCode);

    Float getTickPriceForTicker(String tickerCode);

    Float getMinLotForTicker(String tickerCode);

    List<TrvOrder> getOrdersByTicker(String tickerCode);

    TrvPosition getAvgPositionByTicker(String tickerCode);

    void setOrder(TrvOrder order);

    void openPosition(Signals signal);

    void deleteOrders(String tickerCode);

    Boolean closeAll();
}
