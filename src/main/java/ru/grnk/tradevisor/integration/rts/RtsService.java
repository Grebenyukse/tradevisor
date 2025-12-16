package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class RtsService {

    private final RtsClient rtsClient;

    public void initWhiteList() {
        // do implementation here;
    }

    /**
     * Gets guarantee security (initial margin) for futures contract
     *
     * @param ticker futures contract ticker
     * @return guarantee security value (INITIALMARGIN)
     */
    public Float getGoForFutures(String ticker) {
        try {
            Map<String, Object> responseData = rtsClient.getFuturesContractData(ticker);
            return parseInitialMarginFromResponse(responseData, ticker);
        } catch (Exception e) {
            log.error("Error getting guarantee security for futures contract: {}", ticker, e);
            return 0.0f;
        }
    }
    
    /**
     * Gets guarantee security (initial margin) for futures contract using typed response
     *
     * @param ticker futures contract ticker
     * @return guarantee security value (INITIALMARGIN)
     */
    public Float getGoForFuturesTyped(String ticker) {
        try {
            Map<String, Object> responseData = rtsClient.getTypedFuturesContractData(ticker);
            return parseInitialMarginFromResponse(responseData, ticker);
        } catch (Exception e) {
            log.error("Error getting guarantee security for futures contract: {}", ticker, e);
            return 0.0f;
        }
    }
    
    /**
     * Parses the initial margin value from the MOEX API response
     *
     * @param responseData the response from MOEX API
     * @param ticker the ticker we're looking for
     * @return the initial margin value
     */
    private Float parseInitialMarginFromResponse(Map<String, Object> responseData, String ticker) {
        if (responseData == null || !responseData.containsKey("securities")) {
            log.warn("No securities data found in response for ticker: {}", ticker);
            return 0.0f;
        }
        
        Map<String, Object> securities = (Map<String, Object>) responseData.get("securities");
        List<List<Object>> data = (List<List<Object>>) securities.get("data");
        List<String> columns = (List<String>) securities.get("columns");
        
        if (data == null || data.isEmpty()) {
            log.warn("No data rows found in securities data for ticker: {}", ticker);
            return 0.0f;
        }
        
        // Find the index of INITIALMARGIN column
        int initialMarginIndex = columns.indexOf("INITIALMARGIN");
        if (initialMarginIndex == -1) {
            log.warn("INITIALMARGIN column not found in securities data for ticker: {}", ticker);
            return 0.0f;
        }
        
        // Find the row matching our ticker
        for (List<Object> row : data) {
            String secId = (String) row.get(0); // SECID is typically the first column
            if (ticker.equals(secId)) {
                Object initialMarginObj = row.get(initialMarginIndex);
                if (initialMarginObj instanceof Number) {
                    return ((Number) initialMarginObj).floatValue();
                } else if (initialMarginObj instanceof String) {
                    try {
                        return Float.parseFloat((String) initialMarginObj);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse INITIALMARGIN value '{}' for ticker: {}", initialMarginObj, ticker);
                        return 0.0f;
                    }
                }
            }
        }
        
        log.warn("No matching ticker found in securities data for ticker: {}", ticker);
        return 0.0f;
    }
}
