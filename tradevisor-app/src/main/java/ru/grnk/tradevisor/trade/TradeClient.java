package ru.grnk.tradevisor.trade;

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

    Integer getDigitsForTicker(String tickerCode);

    Boolean isShortAllowedForTicker(String tickerCode);

    List<TrvOrder> getOrdersByTicker(String tickerCode);

    TrvPosition getAvgPositionByTicker(String tickerCode);

    void setOrder(TrvOrder order);

    void openPosition(TrvPosition position);

    void deleteOrder(TrvOrder order);

    Boolean closeAll();
}
