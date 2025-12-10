package ru.grnk.tradevisor.integration.rts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RtsServiceTest {

    @Mock
    private RtsClient rtsClient;

    @InjectMocks
    private RtsService rtsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetGoForFutures_withValidData_returnsCorrectValue() {
        // Given
        String ticker = "RIU4";
        
        // Prepare mock response data that mimics the MOEX API response structure
        Map<String, Object> responseData = createMockResponseData(ticker, 1576.71);
        
        when(rtsClient.getFuturesContractData(ticker)).thenReturn(responseData);

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        assertThat(go).isEqualTo(1576.71f);
    }
    
    @Test
    void testGetGoForFuturesTyped_withValidData_returnsCorrectValue() {
        // Given
        String ticker = "RIU4";
        
        // Prepare mock response data that mimics the MOEX API response structure
        Map<String, Object> responseData = createMockResponseData(ticker, 1576.71);
        
        when(rtsClient.getTypedFuturesContractData(ticker)).thenReturn(responseData);

        // When
        Float go = rtsService.getGoForFuturesTyped(ticker);

        // Then
        assertThat(go).isEqualTo(1576.71f);
    }

    @Test
    void testGetGoForFutures_withInvalidTicker_returnsZero() {
        // Given
        String ticker = "INVALID";
        
        // Prepare mock response data with no matching ticker
        Map<String, Object> responseData = createMockResponseData("RIU4", 1576.71);
        
        when(rtsClient.getFuturesContractData(ticker)).thenReturn(responseData);

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        assertThat(go).isEqualTo(0.0f);
    }

    @Test
    void testGetGoForFutures_withNullResponse_returnsZero() {
        // Given
        String ticker = "RIU4";
        
        when(rtsClient.getFuturesContractData(ticker)).thenReturn(null);

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        assertThat(go).isEqualTo(0.0f);
    }

    @Test
    void testGetGoForFutures_withEmptySecuritiesData_returnsZero() {
        // Given
        String ticker = "RIU4";
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("securities", new HashMap<>());
        
        when(rtsClient.getFuturesContractData(ticker)).thenReturn(responseData);

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        assertThat(go).isEqualTo(0.0f);
    }

    /**
     * Creates a mock response that mimics the structure of the MOEX API response
     */
    private Map<String, Object> createMockResponseData(String ticker, double initialMargin) {
        Map<String, Object> responseData = new HashMap<>();
        
        Map<String, Object> securities = new HashMap<>();
        securities.put("columns", Arrays.asList(
            "SECID", "BOARDID", "SHORTNAME", "SECNAME", "PREVSETTLEPRICE", "DECIMALS", 
            "MINSTEP", "LASTTRADEDATE", "LASTDELDATE", "SECTYPE", "LATNAME", "ASSETCODE", 
            "PREVOPENPOSITION", "LOTVOLUME", "INITIALMARGIN", "HIGHLIMIT", "LOWLIMIT", 
            "STEPPRICE", "LASTSETTLEPRICE", "PREVPRICE", "IMTIME", "BUYSELLFEE", 
            "SCALPERFEE", "NEGOTIATEDFEE", "EXERCISEFEE"
        ));
        
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList(
            ticker, "RFUD", ticker + "-12.25", "Фьючерсный контракт " + ticker + "-12.25",
            66.45, 2, 0.01000, "2025-12-19", "2025-12-19", "F", ticker, ticker,
            60, 1, initialMargin, 70.24000, 62.66000, 0.77900, 66.45, 66.45,
            "2025-12-10 18:59:55", 1.03, 0.52, 0.35, 0.34
        ));
        
        securities.put("data", data);
        responseData.put("securities", securities);
        
        return responseData;
    }
}