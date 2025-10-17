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
        List<MarketData> md = marketDataRepository.fetchMarketDataForLast(100, signal.getInstrumentUuid());
        List<OHLCData> ohlcData = md.stream().map(x -> new OHLCData(x.getTime(), x.getOpen(), x.getHigh(), x.getLow(), x.getClose())).toList();
        Tickers ticker = tickersRepository.findTickerByUid(signal.getInstrumentUuid());
        HorizontalLineDto stopLoss = new HorizontalLineDto(
                signal.getStopLoss(),
                ohlcData.get(0).date(),
                ohlcData.get(ohlcData.size()-1).date(),
                "bold",
                "stop loss mf",
                "red"
        );
        HorizontalLineDto takeProfit = new HorizontalLineDto(
                signal.getStopLoss(),
                ohlcData.get(0).date(),
                ohlcData.get(ohlcData.size()-1).date(),
                "bold",
                "stop loss mf",
                "green"
        );
        HorizontalLineDto priceOpen = new HorizontalLineDto(
                signal.getStopLoss(),
                ohlcData.get(0).date(),
                ohlcData.get(ohlcData.size()-1).date(),
                "dashed",
                "price open",
                "blue"
        );
        PlotRecord plotRecord = new PlotRecord(
            ohlcData, stopLoss, takeProfit, priceOpen, ticker.getTicker(), ticker.getUuid(), signal.getDirection()
        );
        return quickChartService.saveCandlestickChartToFile(plotRecord, true);
    }
}
