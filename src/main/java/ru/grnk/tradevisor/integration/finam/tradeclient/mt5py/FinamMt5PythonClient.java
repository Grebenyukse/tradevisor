package ru.grnk.tradevisor.integration.finam.tradeclient.mt5py;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.finam.tradeclient.OpenPositionClient;
import ru.ttech.piapi.core.helpers.NumberMapper;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.integration.finam.open-position-client", havingValue = "mt5py")
public class FinamMt5PythonClient implements OpenPositionClient {

    private final RestTemplate restTemplate;
    private final TradevisorProperties tradevisorProperties;

    @Override
    public boolean openPosition(String symbol, float priceOpen, float stopLoss, float takeProfit, int direction, int signalId) {
        var baseUrl = tradevisorProperties.integration().finam().mt5PythonClientUrl();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/trade/open-position")
                .toUriString();
        var rq = OpenPositionRq.builder()
                .symbol(symbol)
                .price_open(bigDecimalToPrice(priceOpen))
                .stop_loss(bigDecimalToPrice(stopLoss))
                .take_profit(bigDecimalToPrice(takeProfit))
                .direction(direction)
                .build();

        try {
            var res = restTemplate.postForObject(url, rq, OpenPositionRs.class);

            if (res == null) {
                log.error("Received null response from MT5 Python client for symbol: {}", symbol);
                return false;
            }

            log.info(res.toString());

            // Проверяем, что lot открытой позиции не равен 0
            if (res.lot() == 0) {
                log.error("Position opened with zero lot for symbol: {}", symbol);
                return false;
            }

            return true;

        } catch (HttpClientErrorException e) {
            // Обработка 4XX ошибок
            log.error("Client error ({} {}) when opening position for symbol {}: {}",
                    e.getStatusCode().value(), e.getStatusText(), symbol, e.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException e) {
            // Обработка 5XX ошибок
            log.error("Server error ({} {}) when opening position for symbol {}: {}",
                    e.getStatusCode().value(), e.getStatusText(), symbol, e.getResponseBodyAsString());
            return false;

        } catch (ResourceAccessException e) {
            // Обработка сетевых ошибок
            log.error("Network error when opening position for symbol {}: {}", symbol, e.getMessage());
            return false;

        } catch (Exception e) {
            // Обработка других непредвиденных ошибок
            log.error("Unexpected error when opening position for symbol {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }

    private Price bigDecimalToPrice(float price) {
        var quotation = NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(price));
        return new Price(quotation.getUnits(), quotation.getNano());
    }
}
