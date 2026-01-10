package ru.grnk.tradevisor.integration.bybit;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.TimeInForce;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAssetRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.bybit.dto.CreateOrderResponse;
import ru.grnk.tradevisor.integration.bybit.dto.InstrumentInfoResponse;
import ru.grnk.tradevisor.integration.bybit.dto.OpenOrdersResponse;
import ru.grnk.tradevisor.integration.bybit.dto.PositionResponse;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.math.BigDecimal;
import java.util.List;

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
    private final BybitApiAssetRestClient assetRestClient;
    private final BybitApiMarketRestClient marketRestClient;

    @Override
    public String provider() {
        return "bybit";
    }

    public Float getBalance() {
        return tradevisorProperties.integration().bybit().balance().floatValue();
    }

    public String findTickerForSpot(String tickerCode) {
        return tickerCode.split("@")[0];
    }

    @SneakyThrows
    public InstrumentInfoResponse getInstrumentInfo(String tickerCode) {
            String symbol = findTickerForSpot(tickerCode);
            CategoryType category = getCategoryForTicker(symbol);
            Object response = marketRestClient.getInstrumentsInfo(MarketDataRequest.builder()
                            .category(category)
                            .symbol(symbol)
                    .build());
            String jsonResponse = objectMapper.writeValueAsString(response);
            return objectMapper.readValue(jsonResponse, InstrumentInfoResponse.class);
    }

    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            CategoryType category = getCategoryForTicker(symbol);
            TradeOrderRequest request = TradeOrderRequest.builder()
                    .category(category)
                    .symbol(symbol)
                    .build();

            Object response = tradeRestClient.getOpenOrders(request);
            String jsonResponse = objectMapper.writeValueAsString(response);
            OpenOrdersResponse ordersResponse = objectMapper.readValue(jsonResponse, OpenOrdersResponse.class);

            if (ordersResponse.getResult() != null &&
                    ordersResponse.getResult().getList() != null) {

                return ordersResponse.getResult().getList().stream()
                        .map(this::convertDtoToTrvOrder)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Error fetching orders for ticker: {}", tickerCode, e);
        }
        return List.of();
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            CategoryType category = getCategoryForTicker(symbol);
            if (category == CategoryType.SPOT) {
                // Для спота позиций нет, возвращаем null
                return null;
            }
            // Для деривативов используем эндпоинт позиций
            Object response = tradeRestClient.getOpenOrders(TradeOrderRequest.builder()
                            .category(category)
                            .symbol(symbol)
                    .build());

            String jsonResponse = objectMapper.writeValueAsString(response);
            PositionResponse positionResponse = objectMapper.readValue(jsonResponse, PositionResponse.class);

            if (positionResponse.getResult() != null &&
                    positionResponse.getResult().getList() != null &&
                    !positionResponse.getResult().getList().isEmpty()) {

                PositionResponse.Position position = positionResponse.getResult().getList().get(0);
                if (position.getAvgPrice() != null &&
                        position.getSize() != null &&
                        position.getSide() != null) {

                    return TrvPosition.builder()
                            .tickerCode(tickerCode)
                            .price(BigDecimal.valueOf(Double.parseDouble(position.getAvgPrice())))
                            .lot(Integer.parseInt(position.getSize()))
                            .direction("Buy".equalsIgnoreCase(position.getSide()) ? 1 : -1)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching position for ticker: {}", tickerCode, e);
        }
        return null;
    }

    public String setOrder(TrvOrder order) {
        try {
            var requestBuilder = TradeOrderRequest.builder()
                    .category(getCategoryForTicker(order.tickerCode()))
                    .symbol(order.tickerCode())
                    .side(order.direction() > 0 ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.LIMIT)
                    .qty(String.valueOf(order.lot()))
                    .price(String.valueOf(order.price()))
                    .timeInForce(TimeInForce.GOOD_TILL_CANCEL);
            if (order.activation() != null && order.activation().doubleValue() > 0) {
                requestBuilder.triggerPrice(order.activation().toString());
                requestBuilder.triggerDirection(order.direction() > 0 ? 2 : 1);
            }
            TradeOrderRequest request = requestBuilder.build();
            Object response = tradeRestClient.createOrder(request);
            String jsonResponse = objectMapper.writeValueAsString(response);
            CreateOrderResponse orderResponse = objectMapper.readValue(jsonResponse, CreateOrderResponse.class);
            if (orderResponse.getResult() != null && orderResponse.getResult().getOrderId() != null) {
                return orderResponse.getResult().getOrderId();
            }
            throw new IllegalStateException("нет orderId");
        } catch (Exception e) {
            log.error("Error setting order", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean openPosition(Signals signal) {
        try {
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
            double riskLots =  maxRiskInMoney / normalizedSL.subtract(normalizedPriceOpen).abs().doubleValue();
            int tradeLots = roundPrice(
                    (float) Math.min(availableLots, riskLots),
                    new BigDecimal(lotStep),
                    1)
                    .intValue();
            if (tradeLots == 0) {
                log.warn("Недостаточно средств для открытия позиции. Signal: {}", signal);
                return false;
            }
            String orderId = setOrder(TrvOrder.builder()
                    .tickerCode(tradeTicker.getTicker()) // для выставления позиций используется ticker "без @mic"
                    .direction(signal.getDirection())
                    .price(normalizedPriceOpen)
                    .lot(tradeLots)
                    .isGtc(true)
                    .build());
            if (orderId.isEmpty()) {
                log.error("Не удалось открыть основную позицию. Signal: {}", signal);
                return false;
            }
            // Выставление стоп-лосса
            String stopLossOrderId = setOrder(TrvOrder.builder()
                    .tickerCode(tradeTicker.getTicker())
                    .direction(signal.getDirection() > 0 ? -1 : 1) // Противоположное направление для закрытия
                    .activation(normalizedSL)
                    .price(normalizedSL)
                    .lot(tradeLots)
                    .isGtc(true)
                    .build());
            // Выставление тейк-профита
            String takeProfitOrderId = setOrder(TrvOrder.builder()
                    .tickerCode(tradeTicker.getTicker())
                    .direction(signal.getDirection() > 0 ? -1 : 1) // Противоположное направление для закрытия
                    .activation(normalizedTP)
                    .price(normalizedTP)
                    .lot(tradeLots)
                    .isGtc(true)
                    .build());
            return !stopLossOrderId.isEmpty() && !takeProfitOrderId.isEmpty();
        } catch (Exception e) {
            log.error("Error opening position for signal: {}", signal, e);
            return false;
        }
    }

    @Override
    public void deleteOrders(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            CategoryType category = getCategoryForTicker(symbol);
            TradeOrderRequest request = TradeOrderRequest.builder()
                    .category(category)
                    .symbol(symbol)
                    .build();
            tradeRestClient.cancelAllOrder(request);
        } catch (Exception e) {
            log.error("Error deleting orders for ticker: {}", tickerCode, e);
        }
    }

    private CategoryType getCategoryForTicker(String symbol) {
        if (symbol.endsWith("USDT") && !symbol.contains("-")) {
            return CategoryType.SPOT;
        } else if (symbol.endsWith("USDT") || symbol.endsWith("PERP")) {
            return CategoryType.LINEAR;
        } else {
            return CategoryType.INVERSE;
        }
    }

    private int calculateTradeLots(Signals signal, Tickers ticker, CategoryType category) {
        float balance = getBalance();
        int limits = tradevisorProperties.trade().limits(); // риск в процентах от капитала
        double maxRiskInMoney = balance * limits / 100;
        var instrumentInfo = getInstrumentInfo(ticker.getTicker());
        InstrumentInfoResponse.Instrument instrument = instrumentInfo.getResult().getList().get(0);
        var tickSize = instrument.getPriceFilter().getTickSize();
        var lotStep = instrument.getLotSizeFilter().getMinOrderQty();
        var normalizedPriceOpen = roundPrice(signal.getPriceOpen(), new BigDecimal(tickSize), signal.getDirection());
        var normalizedSL = roundPrice(signal.getStopLoss(), new BigDecimal(tickSize), signal.getDirection());
        double availableLots = (double) balance * LEVERAGE / normalizedPriceOpen.doubleValue();
        double riskLots =  maxRiskInMoney / normalizedSL.subtract(normalizedPriceOpen).abs().doubleValue();
        return roundPrice(
                (float) Math.min(availableLots, riskLots),
                new BigDecimal(lotStep),
                1)
                .intValue();
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
                .lot((int)quantity)
                .isGtc(isGtc)
                .status(status)
                .build();
    }
}
