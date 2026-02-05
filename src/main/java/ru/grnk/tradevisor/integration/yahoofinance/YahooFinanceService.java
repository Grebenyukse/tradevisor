package ru.grnk.tradevisor.integration.yahoofinance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.util.ObjectMapperUtils;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooCandle;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooChartResponse;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooTickerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.yahoofinance.enabled")
class YahooFinanceService {

    private final YahooFinanceClient yahooFinanceClient;


    public List<YahooTickerInfo> fetchAllTickersFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("tickers/yahoofinance_tickers.json");
            String[] symbols = ObjectMapperUtils.readValue(resource.getInputStream(), String[].class);
            List<YahooTickerInfo> tickers = Arrays.stream(symbols)
                    .map(symbol -> new YahooTickerInfo(symbol, symbol, "NYSE", "EQUITY"))
                    .collect(Collectors.toList());
            log.info("Loaded {} tickers from JSON file", tickers.size());
            return tickers;
        } catch (Exception e) {
            log.error("Failed attempt to load tickers from JSON file", e);
            throw new IllegalStateException("Failed to load tickers from JSON file", e);
        }
    }

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
            String[] parts = parseCsvLine(line);
            if (parts.length >= 1) {
                String symbol = parts[0].trim();
                String name = parts.length > 1 ? parts[1].trim() : symbol;
                String exchange = parts.length > 2 ? parts[2].trim() : "";
                String categoryName = parts.length > 3 ? parts[3].trim() : "";
                String country = parts.length > 4 ? parts[4].trim() : "";
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
        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }

    public List<YahooCandle> fetchHistoricalData(String symbol, long from) {
        String period = "1h";
        YahooChartResponse response = yahooFinanceClient.getHistoricalData(symbol, period, from);
        if (response == null || response.chart() == null ||
                response.chart().result() == null || response.chart().result().isEmpty()) {
            log.warn("No data received for symbol: {}", symbol);
            return List.of();
        }
        YahooChartResponse.YahooChartResult result = response.chart().result().get(0);
        List<Long> timestamps = result.timestamp() != null ? result.timestamp() : List.of();
        if (result.indicators() == null || result.indicators().quote() == null ||
                result.indicators().quote().isEmpty()) {
            log.warn("No quote data received for symbol: {}", symbol);
            return List.of();
        }
        YahooChartResponse.YahooQuote quote = result.indicators().quote().get(0);
        List<BigDecimal> opens = quote.open() != null ? quote.open() : List.of();
        List<BigDecimal> highs = quote.high() != null ? quote.high() : List.of();
        List<BigDecimal> lows = quote.low() != null ? quote.low() : List.of();
        List<BigDecimal> closes = quote.close() != null ? quote.close() : List.of();
        int size = Math.min(timestamps.size(),
                Math.min(opens.size(),
                        Math.min(highs.size(),
                                Math.min(lows.size(), closes.size()))));
        if (size == 0) {
            log.warn("No valid data points for symbol: {}", symbol);
            return List.of();
        }
        return IntStream.range(0, size)
                .mapToObj(i -> {
                    Long timestamp = i < timestamps.size() ? timestamps.get(i) : null;
                    BigDecimal open = i < opens.size() ? opens.get(i) : null;
                    BigDecimal high = i < highs.size() ? highs.get(i) : null;
                    BigDecimal low = i < lows.size() ? lows.get(i) : null;
                    BigDecimal close = i < closes.size() ? closes.get(i) : null;

                    return new YahooCandle(
                            timestamp != null ? timestamp * 1000L : 0L, // Преобразуем в миллисекунды
                            open != null ? open.floatValue() : 0f,
                            high != null ? high.floatValue() : 0f,
                            low != null ? low.floatValue() : 0f,
                            close != null ? close.floatValue() : 0f
                    );
                })
                .collect(Collectors.toList());

    }
}
