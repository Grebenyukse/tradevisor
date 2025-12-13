package ru.grnk.tradevisor.integration.bybit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.integration.bybit.dto.BybitOrdersResponse;
import ru.grnk.tradevisor.integration.bybit.dto.BybitWalletBalanceResponse;
import ru.grnk.tradevisor.integration.bybit.BybitTickerRs;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import jakarta.crypto.Mac;
import jakarta.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class BybitTradeClientImpl implements TradeClient {

    private final RestTemplate restTemplate;

    @Value("${app.integration.bybit.url}")
    private String baseUrl;

    @Value("${app.integration.bybit.key}")
    private String apiKey;

    @Value("${app.integration.bybit.secret}")
    private String apiSecret;

    @Override
    public String provider() {
        return "bybit";
    }

    @Override
    public Float getBalance() {
        try {
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/account/wallet-balance";
            String queryString = "accountType=UNIFIED";

            String signature = generateSignature(timestamp, "GET", path, queryString, "", apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BybitWalletBalanceResponse> response = restTemplate.exchange(
                    baseUrl + path + "?" + queryString,
                    HttpMethod.GET,
                    entity,
                    BybitWalletBalanceResponse.class
            );

            if (response.getBody() != null && response.getBody().retCode() == 0) {
                return response.getBody().result().list().stream()
                        .filter(w -> "USDT".equals(w.coin()))
                        .findFirst()
                        .map(w -> w.walletBalance().floatValue())
                        .orElse(0f);
            }
        } catch (Exception e) {
            log.error("Error fetching balance", e);
        }
        return 0f;
    }

    @Override
    public Float getFreeMargin() {
        try {
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/account/wallet-balance";
            String queryString = "accountType=UNIFIED";

            String signature = generateSignature(timestamp, "GET", path, queryString, "", apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BybitWalletBalanceResponse> response = restTemplate.exchange(
                    baseUrl + path + "?" + queryString,
                    HttpMethod.GET,
                    entity,
                    BybitWalletBalanceResponse.class
            );

            if (response.getBody() != null && response.getBody().retCode() == 0) {
                return response.getBody().result().list().stream()
                        .filter(w -> "USDT".equals(w.coin()))
                        .findFirst()
                        .map(w -> w.availableToWithdraw().floatValue())
                        .orElse(0f);
            }
        } catch (Exception e) {
            log.error("Error fetching free margin", e);
        }
        return 0f;
    }

    @Override
    public String findTickerForSpot(String tickerCode) {
        return tickerCode.split("@")[0];
    }

    @Override
    public Float getTickPriceForTicker(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            String url = baseUrl + "/v5/market/instruments-info?category=spot&symbol=" + symbol;

            ResponseEntity<BybitTickerRs> response = restTemplate.getForEntity(url, BybitTickerRs.class);

            if (response.getBody() != null && response.getBody().retCode() == 0) {
                return response.getBody().result().list().stream()
                        .findFirst()
                        .map(info -> info.priceFilter().tickSize().floatValue())
                        .orElse(0f);
            }
        } catch (Exception e) {
            log.error("Error fetching tick price for ticker: {}", tickerCode, e);
        }
        return 0f;
    }

    @Override
    public Float getMinLotForTicker(String tickerCode) {
        try {
            String symbol = findTickerForSpot(tickerCode);
            String url = baseUrl + "/v5/market/instruments-info?category=spot&symbol=" + symbol;

            ResponseEntity<BybitTickerRs> response = restTemplate.getForEntity(url, BybitTickerRs.class);

            if (response.getBody() != null && response.getBody().retCode() == 0) {
                return response.getBody().result().list().stream()
                        .findFirst()
                        .map(info -> info.lotSizeFilter().minOrderQty().floatValue())
                        .orElse(0f);
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
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/order/realtime";
            String queryString = "category=spot&symbol=" + symbol;

            String signature = generateSignature(timestamp, "GET", path, queryString, "", apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BybitOrdersResponse> response = restTemplate.exchange(
                    baseUrl + path + "?" + queryString,
                    HttpMethod.GET,
                    entity,
                    BybitOrdersResponse.class
            );

            if (response.getBody() != null && response.getBody().retCode() == 0) {
                return response.getBody().result().list().stream()
                        .map(this::convertToTrvOrder)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Error fetching orders for ticker: {}", tickerCode, e);
        }
        return List.of();
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        // Bybit spot trading doesn't have positions in the same way as futures
        // For spot, we would need to calculate average buy price from trade history
        return null;
    }

    @Override
    public void setOrder(TrvOrder order) {
        try {
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/order/create";

            Map<String, Object> payload = new HashMap<>();
            payload.put("category", "spot");
            payload.put("symbol", order.tickerCode().split("@")[0]);
            payload.put("side", order.direction() > 0 ? "BUY" : "SELL");
            payload.put("orderType", "LIMIT"); // Assuming LIMIT order, could be enhanced
            payload.put("qty", String.valueOf(order.lot()));

            if (order.price() > 0) {
                payload.put("price", String.valueOf(order.price()));
            }

            String jsonBody = mapToJson(payload);

            String signature = generateSignature(timestamp, "POST", path, "", jsonBody, apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + path,
                    entity,
                    Map.class
            );

            log.info("Set order response: {}", response.getBody());

        } catch (Exception e) {
            log.error("Error setting order", e);
        }
    }

    @Override
    public void openPosition(Signals signal) {
        // For spot trading, opening a position means placing a buy order
        TrvOrder order = new TrvOrder(
                signal.getTickerCode(),
                signal.getDirection(),
                signal.getPriceOpen(),
                signal.getTakeProfit(),
               1,
                true,
                "NEW"
        );
        setOrder(order);
    }

    @Override
    public void deleteOrders(String tickerCode) {

    }

    public void deleteOrder(TrvOrder order) {
        try {
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/order/cancel";

            Map<String, Object> payload = new HashMap<>();
            payload.put("category", "spot");
            payload.put("symbol", order.tickerCode().split("@")[0]);
            // Note: We don't have orderId in TrvOrder, so we'll need to handle this differently
            // For now, we'll skip this implementation as it requires orderId which isn't available

            String jsonBody = mapToJson(payload);

            String signature = generateSignature(timestamp, "POST", path, "", jsonBody, apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            restTemplate.postForEntity(baseUrl + path, entity, Void.class);
        } catch (Exception e) {
            log.error("Error deleting order", e);
        }
    }

    @Override
    public Boolean closeAll() {
        try {
            // Cancel all open orders
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String recvWindow = "5000";
            String path = "/v5/order/cancel-all";

            Map<String, Object> payload = new HashMap<>();
            payload.put("category", "spot");

            String jsonBody = mapToJson(payload);

            String signature = generateSignature(timestamp, "POST", path, "", jsonBody, apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-RECV-WINDOW", recvWindow);
            headers.set("X-BAPI-SIGN", signature);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + path,
                    entity,
                    Map.class
            );

            return response.getBody() != null &&
                    response.getBody().get("retCode") != null &&
                    response.getBody().get("retCode").equals(0);
        } catch (Exception e) {
            log.error("Error closing all positions", e);
            return false;
        }
    }

    private String generateSignature(String timestamp, String method, String path, String params, String body, String secret) {
        try {
            String recvWindow = "5000";
            String toSign = timestamp + apiKey + recvWindow + (method.equals("GET") ? params : (body.isEmpty() ? params : body));
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            byte[] hash = sha256_HMAC.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private TrvOrder convertToTrvOrder(BybitOrdersResponse.OrderInfo orderInfo) {
        // Определяем направление: 1 для покупки (BUY), -1 для продажи (SELL)
        int direction = "BUY".equalsIgnoreCase(orderInfo.side()) ? 1 : -1;

        // Цена (может быть null для рыночных ордеров)
        float price = orderInfo.price() != null ? Float.parseFloat(orderInfo.price()) : 0f;

        // Количество
        float quantity = Float.parseFloat(orderInfo.qty());

        // Note: timeInForce and Status mapping removed as they're not in TrvOrder

        return new TrvOrder(
                orderInfo.symbol() + "@bybit", // Добавляем провайдер к тикеру
                direction,
                price, // Используем цену как уровень активации
                price,
                quantity,
                true, // Defaulting to GTC
                orderInfo.orderStatus() // Сохраняем статус
        );
    }

    // Removed mapOrderStatus method as TrvOrder doesn't have a Status enum

    // Response records for Bybit API responses



}
