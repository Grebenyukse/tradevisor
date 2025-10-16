package ru.grnk.tradevisor.notify.plot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.notify.plot.HorizontalLineDto;
import ru.grnk.tradevisor.notify.plot.OHLCData;
import ru.grnk.tradevisor.notify.plot.PlotRecord;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuickChartService {

    private static final String imageDir = "C:\\Users\\grebe\\IdeaProjects\\tradevisor\\tradevisor-app\\src\\main\\resources\\images";
    private static final String QUICKCHART_URL = "https://quickchart.io/chart/create";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @SneakyThrows
    public void saveCandlestickChartToFile(PlotRecord plotRecord, boolean saveToFs) {
        String chartUrl = createCandlestickChart(plotRecord);
        if (saveToFs && chartUrl != null) {
            downloadAndSaveChart(chartUrl, plotRecord.uuid() + "_" + plotRecord.ticker() + ".png");
        }
        log.info("График сохранён");
    }

    private String createCandlestickChart(PlotRecord plotRecord) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Автоматическое определение размера графика в зависимости от количества данных
            int width = Math.max(800, Math.min(2000, plotRecord.data().size() * 15));
            int height = 600;

            // Создаем объект запроса
            ChartRequestDto requestDto = ChartRequestDto.builder()
                    .width(width)
                    .height(height)
                    .backgroundColor("rgb(255, 255, 255)")
                    .format("png")
                    .chart(createChartDto(plotRecord))
                    .build();

            // Сериализуем в JSON
            String requestBody = mapper.writeValueAsString(requestDto);

            // Отправляем запрос к quickchart.io
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QUICKCHART_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Парсим ответ для получения URL изображения
                ChartResponseDto responseDto = mapper.readValue(response.body(), ChartResponseDto.class);
                return responseDto.getUrl();
            } else {
                log.error("Ошибка при создании графика. Код ответа: {}", response.statusCode());
                log.error("Тело ответа: {}", response.body());
            }

        } catch (Exception e) {
            log.error("Ошибка при создании графика", e);
        }

        return null;
    }

    private ChartDto createChartDto(PlotRecord plotRecord) {
        return ChartDto.builder()
                .type("bar")
                .data(createChartDataDto(plotRecord))
                .options(createChartOptionsDto(plotRecord))
                .build();
    }

    private ChartDataDto createChartDataDto(PlotRecord plotRecord) {
        // Создаем labels
        List<String> labels = plotRecord.data().stream()
                .map(data -> data.date().format(DATE_FORMATTER))
                .collect(Collectors.toList());

        // Создаем dataset
        List<Double> closePrices = plotRecord.data().stream()
                .map(OHLCData::close)
                .collect(Collectors.toList());

        List<String> colors = plotRecord.data().stream()
                .map(data -> data.close() >= data.open() ? "rgba(0, 255, 0, 0.8)" : "rgba(255, 0, 0, 0.8)")
                .collect(Collectors.toList());

        ChartDatasetDto dataset = ChartDatasetDto.builder()
                .label(plotRecord.ticker())
                .data(closePrices)
                .backgroundColor(colors)
                .borderColor(colors)
                .borderWidth(1)
                .build();

        return ChartDataDto.builder()
                .labels(labels)
                .datasets(List.of(dataset))
                .build();
    }

    private ChartOptionsDto createChartOptionsDto(PlotRecord plotRecord) {
        return ChartOptionsDto.builder()
                .responsive(true)
                .maintainAspectRatio(false)
                .plugins(createChartPluginsDto(plotRecord))
                .scales(createChartScalesDto())
                .build();
    }

    private ChartPluginsDto createChartPluginsDto(PlotRecord plotRecord) {
        return ChartPluginsDto.builder()
                .annotation(ChartAnnotationDto.builder()
                        .annotations(createAnnotationsMap(plotRecord))
                        .build())
                .build();
    }

    private ChartScalesDto createChartScalesDto() {
        return ChartScalesDto.builder()
                .x(ChartAxisDto.builder()
                        .ticks(ChartTicksDto.builder()
                                .maxRotation(45)
                                .minRotation(45)
                                .build())
                        .build())
                .y(ChartAxisDto.builder()
                        .beginAtZero(false)
                        .build())
                .build();
    }

    private java.util.Map<String, Object> createAnnotationsMap(PlotRecord plotRecord) {
        java.util.Map<String, Object> annotations = new java.util.HashMap<>();

        // Добавляем уровень Stop Loss
        annotations.put("stoploss", createLineAnnotationDto(plotRecord.stopLoss()));

        // Добавляем уровень Take Profit
        annotations.put("takeprofit", createLineAnnotationDto(plotRecord.takeProfit()));

        // Добавляем уровень Price Open
        annotations.put("priceopen", createLineAnnotationDto(plotRecord.priceOpen()));

        // Добавляем стрелку направления
        if (plotRecord.direction() != null) {
            annotations.put("directionarrow", createDirectionArrowAnnotationDto(plotRecord));
        }

        return annotations;
    }

    private LineAnnotationDto createLineAnnotationDto(HorizontalLineDto lineDto) {
        List<Integer> borderDash = null;
        if ("dashed".equals(lineDto.style())) {
            borderDash = List.of(5, 5);
        } else if ("dotted".equals(lineDto.style())) {
            borderDash = List.of(2, 2);
        }

        return LineAnnotationDto.builder()
                .type("line")
                .mode("horizontal")
                .scaleID("y")
                .value(lineDto.price())
                .borderColor(lineDto.color())
                .borderWidth(2)
                .borderDash(borderDash)
                .label(AnnotationLabelDto.builder()
                        .content(lineDto.label())
                        .enabled(true)
                        .position("start")
                        .build())
                .build();
    }

    private LineAnnotationDto createDirectionArrowAnnotationDto(PlotRecord plotRecord) {
        // Выбираем среднюю точку по оси X для размещения стрелки
        int middleIndex = plotRecord.data().size() / 2;
        String middleDate = plotRecord.data().get(middleIndex).date().format(DATE_FORMATTER);

        String arrowColor = plotRecord.direction() > 0 ? "green" : "red";

        return LineAnnotationDto.builder()
                .type("line")
                .mode("vertical")
                .scaleID("x")
                .value(middleDate)
                .borderColor(arrowColor)
                .borderWidth(3)
                .arrowHeads(AnnotationArrowHeadsDto.builder()
                        .end(AnnotationArrowDto.builder()
                                .display(true)
                                .fill(true)
                                .length(15)
                                .width(10)
                                .build())
                        .build())
                .build();
    }

    private void downloadAndSaveChart(String chartUrl, String filename) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chartUrl))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                Path imagePath = Paths.get(imageDir, filename);
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath, response.body());
                log.info("График успешно сохранен: {}", imagePath.toAbsolutePath());
            } else {
                log.error("Ошибка загрузки изображения. Код ответа: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при сохранении графика", e);
        }
    }

    // DTO для парсинга ответа от quickchart.io
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ChartResponseDto {
        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("url")
        private String url;
    }
}
