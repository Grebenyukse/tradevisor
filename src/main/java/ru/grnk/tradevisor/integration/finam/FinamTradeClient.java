package ru.grnk.tradevisor.integration.finam;

import com.google.type.Decimal;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.collect.prices.LastTickLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.finam.tradeclient.OpenPositionClient;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static grpc.tradeapi.v1.orders.OrderStatus.ORDER_STATUS_CANCELED;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.integration.finam.enabled")
public class FinamTradeClient implements TradeClient {

    private final TradevisorProperties tradevisorProperties;
    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final TickersRepository tickersRepository;
    private final LastTickLoader lastTickLoader;
    private final OpenPositionClient openPositionClient;
    public static final Set<OrderStatus> NOT_ACTIVE_ORDER_STATUSES = Set.of(OrderStatus.ORDER_STATUS_CANCELED, OrderStatus.ORDER_STATUS_FILLED);

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = tradevisorProperties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    @Override
    public void checkPositionStatus(String tickerCode) {
        log.info("check position status completed");
    }

    @Override
    public boolean isPositionOpened(String tickerCode) {
        var bearer = getBearer();
        var accountResponse = accountsServiceBlockingStub
                .withCallCredentials(bearer)
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        var positions = accountResponse.getPositionsList();
        return positions.stream().filter(p -> p.getSymbol().equals(tickerCode))
                .anyMatch(p -> new BigDecimal(p.getQuantity().getValue()).abs().compareTo(BigDecimal.ZERO) > 0);
    }

    @Override
    public String provider() {
        return "finam";
    }


    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        var bearer = getBearer();
        var orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build())
                .getOrdersList()
                .stream()
                .filter(x -> !NOT_ACTIVE_ORDER_STATUSES.contains(x.getStatus()))
                .filter(x -> x.getOrder().getSymbol().equals(tickerCode))
                .map(x -> TrvOrder.builder()
                        .tickerCode(tickerCode)
                        .direction(x.getOrder().getSide() == Side.SIDE_BUY ? 1: -1)
                        .build())
                .toList();
        return orders;
    }

    @Override
    public void deleteOrders(String tickerCode) {
        var bearer = getBearer();
        OrdersResponse orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        var cancelResult = orders.getOrdersList().stream()
                .filter(o -> o.getStatus() != ORDER_STATUS_CANCELED)
                .map(o -> ordersServiceBlockingStub
                        .withCallCredentials(bearer)
                        .cancelOrder(CancelOrderRequest.newBuilder()
                                .setAccountId(tradevisorProperties.integration().finam().accountId())
                                .setOrderId(o.getOrderId())
                                .build()))
                .toList();
        var notCancelledOrders = cancelResult.stream().filter(o -> o.getStatus() != ORDER_STATUS_CANCELED)
                .toList();
        if (!notCancelledOrders.isEmpty()) {
            log.error("не удалось отменить ордера : {}", notCancelledOrders.toString());
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean openPosition(Signals signal) {
        Tickers signalTicker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTradeTickerByTickerCode(signalTicker.getTickerCode());
        if (tradeTicker == null) {
            log.warn("торговый тикер не выставлен. открытие только вручную. SignalId: {}, tickerCode: {}, direction: {}",
                    signal.getId(), signal.getTickerCode(), signal.getDirection());
            return false;
        }
        var kTradeTicker2SpotTicker =
                lastTickLoader.getLastCloseForTicker(tradeTicker.getTickerCode(), tradeTicker.getProvider()) /
                lastTickLoader.getLastCloseForTicker(signalTicker.getTickerCode(), signalTicker.getProvider());
        return openPositionClient.openPosition(
                tradeTicker.getTickerCode(),
                signal.getPriceOpen() * kTradeTicker2SpotTicker,
                signal.getStopLoss() * kTradeTicker2SpotTicker,
                signal.getTakeProfit() * kTradeTicker2SpotTicker,
                signal.getDirection().intValue(),
                signal.getId()
        );
    }

    public static BigDecimal toBigDecimal(Decimal decimal) {
        if (decimal == null) {
            return null;
        }
        if (decimal.getValue().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(decimal.getValue());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal format: " + decimal.getValue(), e);
        }
    }
}