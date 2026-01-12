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
                .queryParam("category", "linear")
                .toUriString();
        BybitTickerRs resp = restTemplate.getForObject(url, BybitTickerRs.class);
        if (resp == null || resp.retCode() != 0) {
            throw new IllegalStateException("Failed to fetch tickers: " + (resp != null ? resp.retMsg() : "null"));
        }
        return resp.result().list();
    }
}
