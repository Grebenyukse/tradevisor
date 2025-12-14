package ru.grnk.tradevisor.integration.bybit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.bybit.enabled")
public class BybitClient {

    private final RestTemplate restTemplate;
    private final TradevisorProperties tradevisorProperties;

    public List<BybitTickerRs.SymbolInfo> fetchAllTickers() {
        var baseUrl = tradevisorProperties.integration().bybit().url();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v5/market/instruments-info")
                .queryParam("category", "spot")
                .toUriString();
        BybitTickerRs resp = restTemplate.getForObject(url, BybitTickerRs.class);
        if (resp == null || resp.retCode() != 0) {
            throw new IllegalStateException("Failed to fetch tickers: " + (resp != null ? resp.retMsg() : "null"));
        }
        return resp.result().list();
    }

    /**
     * Запросить свечи (kline) для конкретного тикера.
     *
     * @param symbol   тикер, например BTCUSDT
     * @param from     начало периода (epoch ms, inclusive)
     * @param limit    максимум свечей в ответе (Bybit ≤ 500)
     * @return список свечей, отсортированных по времени возрастания
     */
    public List<BybitMarketdataRs.Candlestick> fetchHourlyCandles(String tickerCode, long from, int limit) {
        var symbol = tickerCode.split("@")[0];
        var baseUrl = tradevisorProperties.integration().bybit().url();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v5/market/kline")
                .queryParam("category", "spot")
                .queryParam("symbol", symbol)
                .queryParam("interval", "60")
                .queryParam("start", from)
                .queryParam("limit", limit)
                .toUriString();

        BybitMarketdataRs resp = restTemplate.getForObject(url, BybitMarketdataRs.class);
        if (resp == null || resp.retCode() != 0) {
            throw new IllegalStateException("Failed to fetch candles for " + symbol + ": " + (resp != null ? resp.retMsg() : "null"));
        }
        return resp.result().list();
    }
}
