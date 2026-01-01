package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RtsClient {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://iss.moex.com/iss";

    /**
     * Получение данных по фьючерсному контракту из MOEX API
     *
     * @param ticker тикер фьючерсного контракта (например, "RIU4")
     * @return JSON ответ в виде List
     */
    public List<Object> getFuturesContractData(String ticker) {
        String url = String.format("%s/engines/futures/markets/forts/securities.json?iss.meta=off&iss.json=extended&securities.columns=SECID,MINSTEP,STEPPRICE,LOTSIZE,INITIALMARGIN&secid=%s",
                BASE_URL, ticker);
        log.debug("Fetching futures contract data from URL: {}", url);

        try {
            ResponseEntity<List<Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching futures contract data for ticker: {}", ticker, e);
            throw new RuntimeException("Failed to fetch futures contract data for ticker: " + ticker, e);
        }
    }
}
