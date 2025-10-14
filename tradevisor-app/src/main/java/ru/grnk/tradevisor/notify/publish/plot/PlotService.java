package ru.grnk.tradevisor.notify.publish.plot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotRepository repository;
    private final TickersRepository tickersRepository;

    @SneakyThrows
    public void saveCandlestickChartToFile(String uuid, int width, int height) {
        var outputDir = "C:\\Users\\grebe\\IdeaProjects\\tradevisor";
        List<PlotRecord> data = repository.getTickerPlotInfo(uuid);
        Tickers ticker  = tickersRepository.findTickerByUid(uuid);
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No market data found for uuid: " + uuid);
        }

        OHLCDataset dataset = buildDataset(data);

        JFreeChart chart = ChartFactory.createCandlestickChart(
                ticker.getTicker() + " " + detectInterval(data),
                "Time",
                "Price",
                dataset,
                true);

        // Настройка цветов
        XYPlot plot = chart.getXYPlot();
        CandlestickRenderer renderer = (CandlestickRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setUpPaint(Color.GREEN);      // рост
        renderer.setDownPaint(Color.RED);      // падение
        renderer.setDrawVolume(false);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (PlotRecord r : data) {
            if (r.low() < min) min = r.low();
            if (r.high() > max) max = r.high();
        }

        double padding = (max - min) * 0.1;
        if (padding == 0) padding = 0.1;

        rangeAxis.setRange(min - padding, max + padding);
        // Ось времени
        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm"));
        domain.setAutoRange(true);

        // Создаём директорию, если её нет
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Формируем имя файла
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now());
        String fileName = String.format("%s/%s_%s_candlestick.png", outputDir, uuid, timestamp);
        File outputFile = new File(fileName);

        // Сохраняем изображение
        var res = ImageIO.write(chart.createBufferedImage(width, height), "png", outputFile);

        System.out.println("График сохранён: " + outputFile.getAbsolutePath());
    }

    private OHLCDataset buildDataset(List<PlotRecord> rows) {
        int n = rows.size();

        Date[] dates   = new Date[n];
        double[] opens = new double[n];
        double[] highs = new double[n];
        double[] lows  = new double[n];
        double[] closes= new double[n];
        double[] volumes = new double[n];

        for (int i = 0; i < n; i++) {
            PlotRecord r = rows.get(i);
            dates[i]    = Date.from(r.time().toInstant());
            opens[i]    = r.open();
            highs[i]    = r.high();
            lows[i]     = r.low();
            closes[i]   = r.close();
            volumes[i]  = 0.0;
        }

        return new DefaultHighLowDataset(
                rows.get(0).ticker(),
                dates,
                opens,
                highs,
                lows,
                closes,
                volumes);
    }

    private String detectInterval(List<PlotRecord> data) {
        if (data.size() < 2) return "unknown";

        List<Long> diffs = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            long diff = ChronoUnit.SECONDS.between(data.get(i - 1).time(), data.get(i).time());
            diffs.add(diff);
        }

        // Берём минимальную разницу
        long minDiff = diffs.stream().mapToLong(Long::longValue).min().orElse(0);

        // Определяем интервал
        if (minDiff <= 60) return minDiff + "s";
        if (minDiff <= 3600) return (minDiff / 60) + "m";
        if (minDiff <= 86400) return (minDiff / 3600) + "h";
        return (minDiff / 86400) + "d";
    }
}
