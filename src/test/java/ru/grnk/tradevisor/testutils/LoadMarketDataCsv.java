package ru.grnk.tradevisor.testutils;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoadMarketDataCsv {

    public List<MarketData> loadMarketDataFromCsv() {
        List<MarketData> marketDataList = new ArrayList<>();
        String filePath = "csv/market_data_202511242153.csv";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.readLine();
                String line;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXX");
                while ((line = reader.readLine()) != null) {
                    var marketData = getMarketData(line, formatter);
                    marketDataList.add(marketData);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return marketDataList;
    }

    public static @NotNull MarketData getMarketData(String line, DateTimeFormatter formatter) {
        String[] parts = line.replace("\"", "").split(",");
        String dateTimeStr = parts[1]; // time
        OffsetDateTime time = OffsetDateTime.parse(dateTimeStr, formatter);
        return new MarketData(
                parts[0],
                time,
                Float.parseFloat(parts[2]),
                Float.parseFloat(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }
}
