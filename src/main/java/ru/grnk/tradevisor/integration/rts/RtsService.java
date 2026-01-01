package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.integration.rts.dto.ContractParams;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RtsService {

    private final RtsClient rtsClient;

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
        if (responseData == null) {
            log.warn("Response data is null for ticker: {}", ticker);
            return ContractParams.empty(ticker);
        }

        try {
            List<Object> securitiesData = extractSecuritiesData(responseData);

            if (securitiesData == null || securitiesData.size() < 2) {
                log.warn("Invalid securities data structure for ticker: {}", ticker);
                return ContractParams.empty(ticker);
            }

            // Extract columns and data rows
            List<?> columnsRow = (List<?>) securitiesData.get(0);
            List<List<?>> dataRows = (List<List<?>>) securitiesData.get(1);

            // Convert columns to string list
            List<String> columns = columnsRow.stream()
                    .map(Object::toString)
                    .toList();

            int secidIndex = columns.indexOf("SECID");
            if (secidIndex == -1) {
                log.warn("SECID column not found for ticker: {}", ticker);
                return ContractParams.empty(ticker);
            }

            for (List<?> row : dataRows) {
                if (secidIndex < row.size()) {
                    String secId = row.get(secidIndex).toString();
                    if (ticker.equals(secId)) {
                        return ContractParams.builder()
                                .ticker(ticker)
                                .tickSize(parseDecimalValue(row, columns, "MINSTEP"))
                                .tickValue(parseDecimalValue(row, columns, "STEPPRICE"))
                                .lotSize(parseIntegerValue(row, columns, "LOTSIZE"))
                                .initialMargin(parseDecimalValue(row, columns, "INITIALMARGIN"))
                                .build();
                    }
                }
            }

            log.warn("Ticker {} not found in response data", ticker);
            return ContractParams.empty(ticker);

        } catch (Exception e) {
            log.error("Error parsing contract params for ticker: {}", ticker, e);
            return ContractParams.empty(ticker);
        }
    }

    private BigDecimal parseDecimalValue(List<?> row, List<String> columns, String columnName) {
        try {
            int index = columns.indexOf(columnName);
            if (index == -1 || index >= row.size()) {
                return BigDecimal.ZERO;
            }

            Object valueObj = row.get(index);
            if (valueObj == null) {
                return BigDecimal.ZERO;
            }

            if (valueObj instanceof Number) {
                return BigDecimal.valueOf(((Number) valueObj).doubleValue());
            } else {
                return new BigDecimal(valueObj.toString());
            }
        } catch (Exception e) {
            log.debug("Could not parse decimal value for column {}: {}", columnName, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Integer parseIntegerValue(List<?> row, List<String> columns, String columnName) {
        try {
            int index = columns.indexOf(columnName);
            if (index == -1 || index >= row.size()) {
                return 1;
            }

            Object valueObj = row.get(index);
            if (valueObj == null) {
                return 1;
            }

            if (valueObj instanceof Number) {
                return ((Number) valueObj).intValue();
            } else {
                return Integer.parseInt(valueObj.toString());
            }
        } catch (Exception e) {
            log.debug("Could not parse integer value for column {}: {}", columnName, e.getMessage());
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> extractSecuritiesData(List<Object> responseData) {
        for (Object item : responseData) {
            if (item instanceof List) {
                List<?> itemList = (List<?>) item;
                if (!itemList.isEmpty() && "securities".equals(itemList.get(0))) {
                    return (List<Object>) item;
                }
            }
        }
        return null;
    }
}
