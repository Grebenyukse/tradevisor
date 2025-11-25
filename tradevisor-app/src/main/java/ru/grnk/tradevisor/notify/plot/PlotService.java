package ru.grnk.tradevisor.notify.plot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.notify.plot.dto.ChartLineDto;
import ru.grnk.tradevisor.notify.plot.dto.OHLCData;
import ru.grnk.tradevisor.notify.plot.dto.PlotRecord;
import ru.grnk.tradevisor.notify.plot.quickchart.QuickChartService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    public static final int CHART_BARS_NORMAL_COUNT = 600;
    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;
    private final QuickChartService quickChartService;
    private final TradevisorProperties tradevisorProperties;

    private final ObjectMapper om;

    @SneakyThrows
    public String saveCandlestickChartToFile(Signals signal, boolean printRequest) {
        List<MarketData> md = marketDataRepository.fetchMarketDataForLast(
                Math.max(tradevisorProperties.calculate().barsRequiredToCalculateFibo(), CHART_BARS_NORMAL_COUNT),
                signal.getTickerCode()
        );
        List<OHLCData> ohlcData = md.stream().map(x -> new OHLCData(x.getTime(), x.getOpen(), x.getHigh(), x.getLow(), x.getClose())).toList();
        if (ohlcData.isEmpty()) return null;
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        var signalLines = om.readValue(signal.getStrategyProps().toString(), ChartLineDto[].class);
        List<ChartLineDto> lines = new ArrayList<>(Arrays.asList(signalLines));
        ChartLineDto stopLoss = ChartLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("red")
                .fromPrice(signal.getStopLoss())
                .toPrice(signal.getStopLoss())
                .build();
        ChartLineDto takeProfit = ChartLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("green")
                .fromPrice(signal.getTakeProfit())
                .toPrice(signal.getTakeProfit())
                .build();
        ChartLineDto priceOpen = ChartLineDto.builder()
                .fromUtc(ohlcData.get(0).date())
                .toUtc(ohlcData.get(ohlcData.size()-1).date())
                .color("yellow")
                .fromPrice(signal.getPriceOpen())
                .toPrice(signal.getPriceOpen())
                .build();
        PlotRecord plotRecord = new PlotRecord(
                ohlcData,
                stopLoss,
                takeProfit,
                priceOpen,
                ticker.getTicker(),
                ticker.getTickerCode(),
                signal.getDirection(),
                lines
        );
        return quickChartService.getCandlestickChartUrl(plotRecord, printRequest);
    }
}
