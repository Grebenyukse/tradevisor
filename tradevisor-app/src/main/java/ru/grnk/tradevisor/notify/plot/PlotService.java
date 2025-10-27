package ru.grnk.tradevisor.notify.plot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.notify.plot.dto.HorizontalLineDto;
import ru.grnk.tradevisor.notify.plot.dto.OHLCData;
import ru.grnk.tradevisor.notify.plot.dto.PlotRecord;
import ru.grnk.tradevisor.notify.plot.quickchart.QuickChartService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;
    private final QuickChartService quickChartService;

    @SneakyThrows
    public byte[] saveCandlestickChartToFile(Signals signal, boolean saveToFs) {
        List<MarketData> md = marketDataRepository.fetchMarketDataForLast(100, signal.getTickerCode());
        List<OHLCData> ohlcData = md.stream().map(x -> new OHLCData(x.getTime(), x.getOpen(), x.getHigh(), x.getLow(), x.getClose())).toList();
        if (ohlcData.isEmpty()) return "".getBytes(StandardCharsets.UTF_8);
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        HorizontalLineDto stopLoss = HorizontalLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("red")
                .fromPrice(signal.getStopLoss())
                .toPrice(signal.getStopLoss())
                .build();
        HorizontalLineDto takeProfit = HorizontalLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("green")
                .fromPrice(signal.getTakeProfit())
                .toPrice(signal.getTakeProfit())
                .build();
        HorizontalLineDto priceOpen = HorizontalLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("yellow")
                .fromPrice(signal.getPriceOpen())
                .toPrice(signal.getPriceOpen())
                .build();
        PlotRecord plotRecord = new PlotRecord(
            ohlcData, stopLoss, takeProfit, priceOpen, ticker.getTicker(), ticker.getTickerCode(), signal.getDirection()
        );
        return quickChartService.saveCandlestickChartToFile(plotRecord, true);
    }
}
