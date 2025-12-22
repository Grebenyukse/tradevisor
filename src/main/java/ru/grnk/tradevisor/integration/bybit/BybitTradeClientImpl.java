package ru.grnk.tradevisor.integration.bybit;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.asset.request.AssetDataRequest;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.TimeInForce;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAssetRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.bybit.dto.*;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BybitTradeClientImpl implements TradeClient {

    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.integration.bybit.key}")
    private String apiKey;

    @Value("${app.integration.bybit.secret}")
    private String apiSecret;

    @Value("${app.integration.bybit.url}")
    private String baseUrl;

    private BybitApiTradeRestClient tradeRestClient;
    private BybitApiAssetRestClient assetRestClient;
    private BybitApiMarketRestClient marketRestClient;

    @PostConstruct
    public void init() {
        // Определяем домен (mainnet или testnet) на основе baseUrl
        String domain = baseUrl.contains("testnet") ? BybitApiConfig.TESTNET_DOMAIN : BybitApiConfig.MAINNET_DOMAIN;
        boolean debugMode = false; // Включите для отладки

        BybitApiClientFactory factory = BybitApiClientFactory.newInstance(apiKey, apiSecret, domain, debugMode);
        this.tradeRestClient = factory.newTradeRestClient();
        this.assetRestClient = factory.newAssetRestClient();
        this.marketRestClient = factory.newMarketDataRestClient();
    }

    @Override
    public String provider() {
        return "bybit";
    }

    public Float getBalance() {
        try {
            AssetDataRequest request = AssetDataRequest.builder()
                    .accountType(com.bybit.api.client.domain.account.AccountType.UNIFIED)
                    .coin("USDT")
                    .build();

            Object response = assetRestClient.getAssetSingleCoinBalance(request);
            String jsonResponse = objectMapper.writeValueAsString(response);
            BalanceResponse balanceResponse = objectMapper.readValue(jsonResponse, BalanceResponse.class);

            if (balanceResponse.getResult() != null &&
                    balanceResponse.getResult().getList() != null &&
                    !balanceResponse.getResult().getList().isEmpty()) {

                List<BalanceResponse.CoinInfo> coinList = balanceResponse.getResult().getList().get(0).getCoin();
                if (coinList != null) {
                    for (BalanceResponse.CoinInfo coinInfo : coinList) {
                        if ("USDT".equals(coinInfo.getCoin())) {
                            return Float.valueOf(coinInfo.getWalletBalance());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching balance", e);
        }
        return 0f;
    }

    public String findTickerForSpot(String tickerCode) {
        return tickerCode;
    }

    public Float getMinLotForTicker(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            CategoryType category = getCategoryForTicker(symbol);
            Object response = marketRestClient.getInstrumentsInfo(MarketDataRequest.builder()
                            .category(category)
                            .symbol(symbol)
                    .build());
            String jsonResponse = objectMapper.writeValueAsString(response);
            InstrumentInfoResponse instrumentResponse = objectMapper.readValue(jsonResponse, InstrumentInfoResponse.class);

            if (instrumentResponse.getResult() != null &&
                    instrumentResponse.getResult().getList() != null &&
                    !instrumentResponse.getResult().getList().isEmpty()) {

                InstrumentInfoResponse.Instrument instrument = instrumentResponse.getResult().getList().get(0);
                if (instrument.getLotSizeFilter() != null) {
                    return Float.valueOf(instrument.getLotSizeFilter().getMinOrderQty());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching min lot for ticker: {}", tickerCode, e);
        }
        return 0f;
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
                            .price(Float.valueOf(position.getAvgPrice()))
                            .lot(Float.valueOf(position.getSize()))
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
            if (order.activation() != null && order.activation() > 0) {
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
            String symbol = tradeTicker.getTicker();
            CategoryType category = getCategoryForTicker(symbol);
            int tradeLots = calculateTradeLots(signal, tradeTicker, category);
            if (tradeLots == 0) {
                log.warn("Недостаточно средств для открытия позиции. Signal: {}", signal);
                return false;
            }
            String orderId = setOrder(TrvOrder.builder()
                    .tickerCode(tradeTicker.getTicker()) // для выставления позиций используется ticker "без @mic"
                    .direction(signal.getDirection())
                    .price(signal.getPriceOpen())
                    .lot((float) tradeLots)
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
                    .activation(signal.getStopLoss())
                    .price(signal.getStopLoss())
                    .lot((float) tradeLots)
                    .isGtc(true)
                    .build());

            // Выставление тейк-профита
            String takeProfitOrderId = setOrder(TrvOrder.builder()
                    .tickerCode(tradeTicker.getTicker())
                    .direction(signal.getDirection() > 0 ? -1 : 1) // Противоположное направление для закрытия
                    .activation(signal.getTakeProfit())
                    .price(signal.getTakeProfit())
                    .lot((float) tradeLots)
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
        // Проверяем, что tradevisorProperties.trade().limits() не null
        Integer limits = tradevisorProperties.trade().limits();
        if (limits == null) {
            limits = 1; // Значение по умолчанию
        }
        double maxRiskInMoney = balance * limits / 100;
        Float minLot = getMinLotForTicker(ticker.getSpotTickerCode() + "@bybit");

        // Проверяем, что minLot не null
        if (minLot == null || minLot <= 0) {
            minLot = 1.0f; // Значение по умолчанию
        }

        if (category == CategoryType.SPOT) {
            float availableBalance = getBalance();
            double availableLots = availableBalance / signal.getPriceOpen();
            double stopLossRisk = Math.abs(signal.getStopLoss() - signal.getPriceOpen());
            double riskBasedLots = maxRiskInMoney / stopLossRisk;
            // Используем Math.min чтобы выбрать более консервативное значение
            double minLots = Math.min(availableLots, riskBasedLots);
            // Округляем вниз до ближайшего целого кратного minLot
            return (int) (Math.floor(minLots / minLot) * minLot);
        } else {
            // Для деривативов учитываем плечо (упрощённо)
            float marginPerLot = signal.getPriceOpen() * minLot;
            double availableLots = balance / marginPerLot;
            double stopLossRisk = Math.abs(signal.getStopLoss() - signal.getPriceOpen()) * minLot;
            double riskBasedLots = maxRiskInMoney / stopLossRisk;
            // Используем Math.min чтобы выбрать более консервативное значение
            double minLots = Math.min(availableLots, riskBasedLots);
            // Округляем вниз до ближайшего целого кратного minLot
            return (int) (Math.floor(minLots / minLot) * minLot);
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
                .price(price)
                .lot(quantity)
                .isGtc(isGtc)
                .status(status)
                .build();
    }
}
