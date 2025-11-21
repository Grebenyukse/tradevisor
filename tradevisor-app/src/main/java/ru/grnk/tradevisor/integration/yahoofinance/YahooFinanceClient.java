package ru.grnk.tradevisor.integration.yahoofinance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.util.ObjectMapperUtils;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooCandle;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooChartResult;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooQuote;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooTickerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.yahoofinance.enabled")
class YahooFinanceClient {

    private final YahooFinanceService yahooFinanceService;

    public List<YahooTickerInfo> fetchAllTickers() {
        try {
            ClassPathResource resource = new ClassPathResource("tickers/yahoofinance.csv");
            List<YahooTickerInfo> tickers = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String headerLine = reader.readLine();
                log.debug("CSV Header: {}", headerLine);
                String line;
                while ((line = reader.readLine()) != null) {
                    processLine(line, tickers);
                }
            }
            log.info("Loaded {} tickers from CSV file", tickers.size());
            return tickers;
        } catch (Exception e) {
            log.error("Failed to load tickers from CSV file", e);
            throw new IllegalStateException("Failed to load tickers from CSV file", e);
        }
    }

    private void processLine(String line, List<YahooTickerInfo> tickers) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        try {
            // Разбираем строку CSV вручную, учитывая возможные кавычки
            String[] parts = parseCsvLine(line);

            if (parts.length >= 1) {
                String symbol = parts[0].trim();
                String name = parts.length > 1 ? parts[1].trim() : symbol;
                String exchange = parts.length > 2 ? parts[2].trim() : "";
                String categoryName = parts.length > 3 ? parts[3].trim() : "";
                String country = parts.length > 4 ? parts[4].trim() : "";

                // Создаем тип на основе категории или страны
                String type = "EQUITY";
                if (!categoryName.isEmpty()) {
                    type = categoryName;
                } else if (!country.isEmpty()) {
                    type = country + "_EQUITY";
                }

                tickers.add(new YahooTickerInfo(symbol, name, exchange, type));
            }
        } catch (Exception e) {
            log.warn("Failed to parse ticker line: {}", line, e);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(ch);
            }
        }

        // Добавляем последнее поле
        result.add(currentField.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Запросить исторические данные (candles) для конкретного тикера.
     *
     * @param symbol  тикер, например AAPL
     * @param period1 начало периода (epoch seconds)
     * @param period2 конец периода (epoch seconds)
     * @return список свечей
     */
    public List<YahooCandle> fetchHistoricalData(String symbol, long period1, long period2) {
        try {
            String response = yahooFinanceService.getHistoricalData(symbol, "1h", 5);
            YahooChartResult result = ObjectMapperUtils.readValue(response, YahooChartResult.class);
            if (result.timestamp() == null || result.indicators() == null
                    || result.indicators().quote() == null || result.indicators().quote().isEmpty()) {
                return List.of();
            }
            YahooQuote quote = result.indicators().quote().get(0);
            List<Long> timestamps = result.timestamp();
            List<BigDecimal> opens = quote.open();
            List<BigDecimal> highs = quote.high();
            List<BigDecimal> lows = quote.low();
            List<BigDecimal> closes = quote.close();

            return IntStream.range(0, timestamps.size())
                    .mapToObj(i -> new YahooCandle(
                            timestamps.get(i) * 1000L, // Преобразуем в миллисекунды
                            opens.size() > i && opens.get(i) != null ? opens.get(i).floatValue() : 0f,
                            highs.size() > i && highs.get(i) != null ? highs.get(i).floatValue() : 0f,
                            lows.size() > i && lows.get(i) != null ? lows.get(i).floatValue() : 0f,
                            closes.size() > i && closes.get(i) != null ? closes.get(i).floatValue() : 0f
                    ))
                    .collect(toList());
        } catch (Exception e) {
            log.error("Failed to fetch historical data for symbol: {}", symbol, e);
            return List.of();
        }
    }
}
