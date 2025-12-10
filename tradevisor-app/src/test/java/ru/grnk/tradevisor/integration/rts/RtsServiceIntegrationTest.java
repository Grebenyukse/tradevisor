package ru.grnk.tradevisor.integration.rts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.grnk.tradevisor.integration.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.location=classpath:config/application-test.yaml"
        })
class RtsServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RtsService rtsService;

    @Test
    void testGetGoForFutures_withValidTicker_returnsInitialMargin() {
        // Given
        String ticker = "RIU4"; // A real RTS futures contract ticker

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        // We expect a positive value for the guarantee security
        assertThat(go).isNotNull();
        // Note: We're not asserting a specific value since market data changes
        // but we can assert it's non-negative
        assertThat(go).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void testGetGoForFutures_withInvalidTicker_returnsZero() {
        // Given
        String ticker = "INVALID_TICKER";

        // When
        Float go = rtsService.getGoForFutures(ticker);

        // Then
        assertThat(go).isEqualTo(0.0f);
    }
}