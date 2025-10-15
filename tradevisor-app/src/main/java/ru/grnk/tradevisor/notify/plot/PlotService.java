package ru.grnk.tradevisor.notify.plot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotRepository repository;
    private final TickersRepository tickersRepository;


    @SneakyThrows
    public byte[] getCandlestickChartAsBytes(String uuid, int width, int height) {
        List<PlotRecord> data = repository.getTickerPlotInfo(uuid);
        JFreeChart chart = createCandlestickChart(uuid, data);
        BufferedImage image = chart.createBufferedImage(width, height);
        return bufferedImageToByteArray(image, "png");
    }

    @SneakyThrows
    public void saveCandlestickChartToFile(String uuid, int width, int height) {
        String outputDir = "C:\\Users\\grebe\\IdeaProjects\\tradevisor\\tradevisor-app\\src\\main\\resources\\images";
        List<PlotRecord> data = repository.getTickerPlotInfo(uuid);
        Tickers tickerInfo = tickersRepository.findTickerByUid(uuid);
        JFreeChart chart = createCandlestickChart(uuid, data);
        BufferedImage image = chart.createBufferedImage(width, height);
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now());
        String fileName = String.format("%s/%s_%s_candlestick.png", outputDir, tickerInfo.getTicker(), timestamp);
        File outputFile = new File(fileName);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) {
                throw new IOException("Не найден ImageWriter для формата 'png'");
            }
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.95f);
            }
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
        }
        log.info("График сохранён: {}", outputFile.getAbsolutePath());
    }

    private JFreeChart createCandlestickChart(String ticker, List<PlotRecord> data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No market data found for ticker: " + ticker);
        }
        String interval = detectInterval(data);
        OHLCDataset dataset = buildDataset(data);
        JFreeChart chart = ChartFactory.createCandlestickChart(
                ticker + " – " + interval + " Candlestick Chart",
                "Time",
                "Price",
                dataset,
                true);
        XYPlot plot = chart.getXYPlot();
        CandlestickRenderer renderer = (CandlestickRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setUpPaint(Color.GREEN);
        renderer.setDownPaint(Color.RED);
        renderer.setDrawVolume(false);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double min = data.stream().mapToDouble(PlotRecord::low).min().orElse(0);
        double max = data.stream().mapToDouble(PlotRecord::high).max().orElse(1);
        double padding = (max - min) * 0.01;
        if (padding == 0) padding = 0.01;
        rangeAxis.setRange(min - padding, max + padding);
        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm"));
        domain.setAutoRange(true);
        return chart;
    }

    private byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IOException("Не найден ImageWriter для формата '" + format + "'");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.95f);
            }

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
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
