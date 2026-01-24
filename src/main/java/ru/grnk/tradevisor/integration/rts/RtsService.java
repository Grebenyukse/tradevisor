package ru.grnk.tradevisor.integration.rts;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.integration.rts.dto.ContractParams;
import ru.grnk.tradevisor.integration.rts.dto.SecuritiesData;
import ru.grnk.tradevisor.integration.rts.dto.Security;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RtsService {

    private final RtsClient rtsClient;
    private final ObjectMapper objectMapper;

    /**
     * Получение всех параметров контракта за один запрос
     *
     * @param ticker тикер фьючерсного контракта
     * @return ContractParams с параметрами контракта
     */
    public ContractParams getContractParams(String ticker) {
        try {
            List<Object> responseData = rtsClient.getFuturesContractData(ticker);
            return parseContractParams(responseData, ticker);
        } catch (Exception e) {
            log.error("Error getting contract params for ticker: {}", ticker, e);
            return ContractParams.empty(ticker);
        }
    }

    private ContractParams parseContractParams(List<Object> responseData, String ticker) {
        if (responseData == null || responseData.isEmpty()) {
            log.warn("Response data is null or empty for ticker: {}", ticker);
            return ContractParams.empty(ticker);
        }

        try {
            // Находим объект с securities
            SecuritiesData securitiesData = findSecuritiesData(responseData);

            if (securitiesData == null || securitiesData.getSecurities() == null) {
                log.warn("No securities data found for ticker: {}", ticker);
                return ContractParams.empty(ticker);
            }

            // Ищем нужный контракт по тикеру
            for (Security security : securitiesData.getSecurities()) {
                if (ticker.equals(security.getSecid())) {
                    return ContractParams.builder()
                            .ticker(ticker)
                            .tickSize(security.getMinstep() != null ? security.getMinstep() : BigDecimal.ZERO)
                            .tickValue(security.getStepPrice() != null ? security.getStepPrice() : BigDecimal.ZERO)
                            .lotSize(security.getLotSize() != null ? security.getLotSize() : 1)
                            .initialMargin(security.getInitialMargin() != null ? security.getInitialMargin() : BigDecimal.ZERO)
                            .build();
                }
            }

            log.warn("Ticker {} not found in response data", ticker);
            return ContractParams.empty(ticker);

        } catch (Exception e) {
            log.error("Error parsing contract params for ticker: {}", ticker, e);
            return ContractParams.empty(ticker);
        }
    }

    @SuppressWarnings("unchecked")
    private SecuritiesData findSecuritiesData(List<Object> responseData) {
        try {
            for (Object item : responseData) {
                if (item instanceof java.util.Map<?, ?> map) {
                    if (map.containsKey("securities")) {
                        return objectMapper.convertValue(map, SecuritiesData.class);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error converting securities data", e);
        }
        return null;
    }
}
