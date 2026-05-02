package ru.grnk.tradevisor.testutils;

import lombok.SneakyThrows;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class GenCandlesUtils {


    @SneakyThrows
    public static List<MarketData> readMarketDataFromFile(String filePath, String tickerCode) {
        OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.UTC);
        return Files.lines(Paths.get(filePath))
                .skip(1) // Пропускаем заголовок
                .map(line -> line.split(";"))
                .map(parts -> {
                    // Заменяем запятые на точки для корректного парсинга во Float
                    float low = Float.parseFloat(parts[1].replace(",", "."));
                    float open = Float.parseFloat(parts[2].replace(",", "."));
                    float close = Float.parseFloat(parts[3].replace(",", "."));
                    float high = Float.parseFloat(parts[4].replace(",", "."));

                    // Преобразуем порядковый номер строки в метку времени
                    long hoursToAdd = Long.parseLong(parts[0]);
                    OffsetDateTime rowTime = baseTime.plusHours(hoursToAdd);

                    return new MarketData(
                            tickerCode,
                            rowTime,
                            open,
                            high,
                            low,
                            close
                    );
                })
                .collect(Collectors.toList())
                .reversed();
    }
}
