package ru.grnk.tradevisor.integration.bybit;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.TriggerBy;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.account.request.AccountDataRequest;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.domain.position.TpslMode;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.domain.trade.PositionIdx;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.TimeInForce;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.util.ObjectMapperUtils;
import ru.grnk.tradevisor.integration.bybit.dto.*;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static ru.grnk.tradevisor.common.util.RoundPriceUtils.roundPrice;

@Component
@RequiredArgsConstructor
@Slf4j
public class BybitTradeClientImpl implements TradeClient {

    public static final int LEVERAGE = 10;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BybitApiTradeRestClient tradeRestClient;
    private final BybitApiMarketRestClient marketRestClient;
    private final BybitApiAccountRestClient bybitApiAccountRestClient;
    private final BybitApiPositionRestClient bybitApiPositionRestClient;

    @Override
    public String provider() {
        return "bybit";
    }

    public Float getBalance() {
        var resp = bybitApiAccountRestClient.getWalletBalance(AccountDataRequest.builder()
                .accountType(AccountType.UNIFIED)
                .build());
        var accountInfo = ObjectMapperUtils.readValue(ObjectMapperUtils.writeValue(resp), BybitAccountInfoResponse.class);
        return accountInfo.result().list().stream().findFirst().orElseThrow().totalAvailableBalance().floatValue();
    }

    public String findTickerForSpot(String tickerCode) {
        return tickerCode.split("@")[0];
    }

    @SneakyThrows
    public InstrumentInfoResponse getInstrumentInfo(String tickerCode) {
        String symbol = findTickerForSpot(tickerCode);
        Object response = marketRestClient.getInstrumentsInfo(MarketDataRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(symbol)
                .build());
        String jsonResponse = objectMapper.writeValueAsString(response);
        return objectMapper.readValue(jsonResponse, InstrumentInfoResponse.class);
    }

    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        Object response = tradeRestClient.getOpenOrders(TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(findTickerForSpot(tickerCode))
                .build());
        OpenOrdersResponse ordersResponse = ObjectMapperUtils.readValue(ObjectMapperUtils.writeValue(response), OpenOrdersResponse.class);
        return ordersResponse.getResult().getList().stream()
                .map(this::convertDtoToTrvOrder)
                .toList();
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        var res = bybitApiPositionRestClient.getPositionInfo(PositionDataRequest.builder()
                .baseCoin(tickerCode.split("USDT")[0])
                .settleCoin("USDT")
                .category(CategoryType.LINEAR)
                .build());
        var positions = ObjectMapperUtils.readValue(ObjectMapperUtils.writeValue(res), PositionResponse.class);
        return positions.result().list().stream()
                .filter(x -> Objects.equals(x.symbol(), tickerCode.split("@")[0]))
                .findFirst()
                .map(p -> TrvPosition.builder()
                        .tickerCode(tickerCode)
                        .price(p.avgPrice())
                        .lot(p.size().intValue())
                        .direction("Buy".equalsIgnoreCase(p.side()) ? 1 : -1)
                        .build())
                .orElse(null);
    }

    @SneakyThrows
    @Override
    public boolean openPosition(Signals signal) {
        Tickers spotTicker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTradeTickerByTickerCodeIfExists(spotTicker.getTickerCode())
                .orElse(spotTicker);
        float balance = getBalance();
        int limits = tradevisorProperties.trade().limits(); // риск в процентах от капитала
        double maxRiskInMoney = balance * limits / 100;
        var instrumentInfo = getInstrumentInfo(tradeTicker.getTicker());
        InstrumentInfoResponse.Instrument instrument = instrumentInfo.getResult().getList().get(0);
        var tickSize = instrument.getPriceFilter().getTickSize();
        var lotStep = instrument.getLotSizeFilter().getMinOrderQty();
        var normalizedPriceOpen = roundPrice(signal.getPriceOpen(), new BigDecimal(tickSize), signal.getDirection());
        var normalizedSL = roundPrice(signal.getStopLoss(), new BigDecimal(tickSize), signal.getDirection());
        var normalizedTP = roundPrice(signal.getTakeProfit(), new BigDecimal(tickSize), signal.getDirection());
        double availableLots = (double) balance * LEVERAGE / normalizedPriceOpen.doubleValue();
        double riskLots = maxRiskInMoney / normalizedSL.subtract(normalizedPriceOpen).abs().doubleValue();
        int tradeLots = roundPrice(
                (float) Math.min(availableLots, riskLots),
                new BigDecimal(lotStep),
                1)
                .intValue();
        if (tradeLots == 0) {
            log.warn("Недостаточно средств для открытия позиции. Signal: {}", signal);
            return false;
        }
        var request = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(signal.getTickerCode().split("@")[0])
                .side(signal.getDirection() > 0 ? Side.BUY : Side.SELL)
                .orderType(TradeOrderType.LIMIT)
                .qty(String.valueOf(tradeLots))
                .price(String.valueOf(normalizedPriceOpen))
                .timeInForce(TimeInForce.GOOD_TILL_CANCEL)
                .takeProfit(String.valueOf(normalizedTP))
                .stopLoss(String.valueOf(normalizedSL))
                .tpOrderType(TradeOrderType.LIMIT)
                .slOrderType(TradeOrderType.LIMIT)
                .tpLimitPrice(String.valueOf(normalizedTP))
                .slLimitPrice(String.valueOf(normalizedSL))
                .tpslMode(TpslMode.PARTIAL.name())
                .tpTriggerBy(TriggerBy.MARK_PRICE)
                .slTriggerBy(TriggerBy.MARK_PRICE)
                .positionIdx(PositionIdx.ONE_WAY_MODE)
                .build();
        Object response = tradeRestClient.createOrder(request);
        String jsonResponse = objectMapper.writeValueAsString(response);
        CreateOrderResponse orderResponse = objectMapper.readValue(jsonResponse, CreateOrderResponse.class);
        if (orderResponse.getResult() != null && orderResponse.getResult().getOrderId() != null) {
            log.info("order has been set. signal: {} orderId: {}", signal, orderResponse.getResult().getOrderId());
            return true;
        }
        throw new IllegalStateException("нет orderId");
    }

    @Override
    public void deleteOrders(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            TradeOrderRequest request = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(symbol)
                    .build();
            tradeRestClient.cancelAllOrder(request);
        } catch (Exception e) {
            log.error("Error deleting orders for ticker: {}", tickerCode, e);
        }
    }

    private TrvOrder convertDtoToTrvOrder(OpenOrdersResponse.Order order) {
        int direction = "Buy".equalsIgnoreCase(order.getSide()) ? 1 : -1;
        float price = order.getPrice() != null ? Float.parseFloat(order.getPrice()) : 0f;
        float quantity = order.getQty() != null ? Float.parseFloat(order.getQty()) : 0f;
        boolean isGtc = "GTC".equalsIgnoreCase(order.getTimeInForce());
        String status = order.getOrderStatus() != null ? order.getOrderStatus() : "";
        String tickerCode = order.getSymbol() != null ? order.getSymbol() + "@bybit" : "";
        return TrvOrder.builder()
                .tickerCode(tickerCode)
                .direction(direction)
                .price(BigDecimal.valueOf(price))
                .lot((int) quantity)
                .isGtc(isGtc)
                .status(status)
                .build();
    }
}
