package ru.grnk.tradevisor.notify.plot;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.Signals;
import ru.grnk.tradevisor.dbmodel.tables.Tickers;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PlotRepository {

    private final DSLContext dsl;

    public List<PlotRecord> getTickerPlotInfo(String uuid) {
        var ohlcd =  dsl.select(
                MarketData.MARKET_DATA.OPEN,
                MarketData.MARKET_DATA.HIGH,
                MarketData.MARKET_DATA.LOW,
                MarketData.MARKET_DATA.CLOSE,
                MarketData.MARKET_DATA.TIME
                ).from(MarketData.MARKET_DATA)
                .where(MarketData.MARKET_DATA.INSTRUMENT_UUID.eq(uuid))
                .fetchStreamInto(OHLCData.class)
                .toList();

        var tickerInfo = dsl.select(
                Tickers.TICKERS.TICKER,
                Tickers.TICKERS.UUID
        );



        return List.of();
    }
}
