package ru.grnk.tradevisor.notify.plot.quickchart;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.notify.plot.dto.PlotRecord;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.grnk.tradevisor.common.util.MathUtils.round;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuickChartService {

    private static final String imageDir = "C:\\Users\\grebe\\IdeaProjects\\tradevisor\\tradevisor-app\\src\\main\\resources\\images";
    private static final String QUICKCHART_URL = "https://quickchart.io/chart/create";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public byte[] saveCandlestickChartToFile(PlotRecord plotRecord, boolean saveToFs) {
        String chartUrl = createCandlestickChart(plotRecord);
        if (chartUrl == null) {
            log.error("не получен урл графика");
            return null;
        }
        byte[] downloadedChart = download(chartUrl);
        if (saveToFs && downloadedChart != null) {
            String path = saveChartToFile(downloadedChart.clone(), plotRecord.uuid() + "_" + plotRecord.ticker() + ".png");
            log.info("график сохранен локально по адресу: {}", path);
        }
        return downloadedChart;
    }

    @SneakyThrows
    private String createCandlestickChart(PlotRecord plotRecord) {
        int width = Math.max(800, Math.min(2000, plotRecord.data().size() * 15));
        int height = 600;
        ChartRequestDto requestDto = ChartRequestDto.builder()
                .width(width)
                .height(height)
                .format("png")
                .version(3)
                .chart(createChartDto(plotRecord))
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(QUICKCHART_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            ChartResponseDto responseDto = objectMapper.readValue(response.body(), ChartResponseDto.class);
            return responseDto.getUrl();
        } else {
            log.error("Ошибка при создании графика. Код ответа: {}", response.statusCode());
            log.error("Тело ответа: {}", response.body());
            throw new RuntimeException("Ошибка при создании графика");
        }
    }

    private ChartDto createChartDto(PlotRecord plotRecord) {
        return ChartDto.builder()
                .type("ohlc")
                .data(ChartDto.ChartData.builder()
                        .datasets(List.of(
                                ChartDto.ChartData.ChartDataset.builder()
                                        .label(plotRecord.ticker())
                                        .yAxisID("y")
                                        .color(ChartDto.ChartData.ChartDataset.OhlcColor.builder()
                                                .up("#aaa")
                                                .down("#000")
                                                .unchanged("#ccc")
                                                .build())
                                        .data(plotRecord.data().stream().map(x -> ChartDto.ChartData.ChartDataset.Dataset.builder()
                                                .x(x.date().toEpochSecond() * 1000)
                                                .o(round(x.open(), 4))
                                                .h(round(x.high(), 4))
                                                .l(round(x.low(), 4))
                                                .c(round(x.close(), 4))
                                                .build()
                                        ).toList())
                                        .build()
                        ))
                        .build())
                .options(ChartDto.ChartOptions.builder()
                        .scales(ChartDto.ChartOptions.ChartScales.builder()
                                .x(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.builder()
                                        .adapters(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.ChartAdapters.builder()
                                                .date(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.ChartAdapters.ChartZone.builder()
                                                        .zone("UTC-3")
                                                        .build())
                                                .build())
                                        .time(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.ChartTime.builder()
                                                .displayFormats(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.ChartTime.DisplayFormats.builder()
                                                        .day("MMM d")
                                                        .month("MMM d")
                                                        .build())
                                                .stepSize(1)
                                                .unit("day")
                                                .build())
                                        .build())
                                .y1(ChartDto.ChartOptions.ChartScales.ChartOptionsScale.builder()
                                        .stack("stockChart")
                                        .stackWeight(10)
                                        .weight(2)
                                        .build())
                                .build())
                        .plugins(ChartDto.ChartOptions.ChartPlugins.builder()
                                .annotation(ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.builder()
                                        .annotations(getAnnotations(plotRecord))
                                        .build()
                                )
                                .build())
                        .build())
                .build();

    }

    private static List<ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.ChartAnnotationItem> getAnnotations(PlotRecord plotRecord) {
        List<ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.ChartAnnotationItem> res = new ArrayList<>();
        for (var hl : plotRecord.lines()) {
            res.add(ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.ChartAnnotationItem.builder()
                    .type("line")
                    .mode("horizontal")
                    .yMin(Math.min(hl.fromPrice(), hl.toPrice()))
                    .yMax(Math.max(hl.toPrice(), hl.fromPrice()))
                    .xMax(hl.fromUtc().toEpochSecond() * 1000)
                    .xMin(hl.toUtc().toEpochSecond() * 1000)
                    .borderColor(hl.color())
                    .label(ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.ChartAnnotationItem.Label.builder()
                            .enabled(true)
                            .color("black")
                            .backgroundColor("transparent")
                            .content(hl.label())
                            .position("end")
                            .padding(ChartDto.ChartOptions.ChartPlugins.ChartAnnotation.ChartAnnotationItem.Label.Padding.builder()
                                    .bottom(15)
                                    .build())
                            .build())
                    .build());
        }
        return res;
    }

    private byte[] download(String chartUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chartUrl))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                log.info("График успешно скачен");
                return response.body();
            } else {
                log.error("Ошибка загрузки изображения. Код ответа: {}", response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при сохранении графика", e);
            throw new RuntimeException("Ошибка при сохранении графика", e);
        }
    }

    private String saveChartToFile(byte[] chartBytes, String filename) {
        try {
            Path imagePath = Paths.get(imageDir, filename);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, chartBytes);
            log.info("График успешно сохранен: {}", imagePath.toAbsolutePath());
            return imagePath.toString();
        } catch (IOException e) {
            log.error("Ошибка при сохранении графика", e);
            throw new RuntimeException("Ошибка при сохранении графика", e);
        }
    }
}
