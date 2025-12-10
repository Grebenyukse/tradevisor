package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RtsClient {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://iss.moex.com/iss";

    /**
     * Fetches futures contract data from MOEX API by ticker
     *
     * @param ticker futures contract ticker (e.g., "RIU4")
     * @return JSON response as Map
     */
    public Map<String, Object> getFuturesContractData(String ticker) {
        String url = String.format("%s/engines/futures/markets/forts/securities.json?secid=%s", BASE_URL, ticker);
        log.debug("Fetching futures contract data from URL: {}", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching futures contract data for ticker: {}", ticker, e);
            throw new RuntimeException("Failed to fetch futures contract data for ticker: " + ticker, e);
        }
    }
    
    /**
     * Fetches futures contract data from MOEX API by ticker with improved type handling
     *
     * @param ticker futures contract ticker (e.g., "RIU4")
     * @return JSON response parsed with better type safety
     */
    public Map<String, Object> getTypedFuturesContractData(String ticker) {
        String url = String.format("%s/engines/futures/markets/forts/securities.json?secid=%s", BASE_URL, ticker);
        log.debug("Fetching futures contract data from URL: {}", url);
        
        try {
            // Using ParameterizedTypeReference for better type safety
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
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
