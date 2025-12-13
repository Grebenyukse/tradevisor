package ru.grnk.tradevisor.common.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.Tickers;
import ru.grnk.tradevisor.notify.plot.dto.OHLCData;
import ru.grnk.tradevisor.notify.plot.dto.PlotRecord;

import java.util.List;

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
                .where(MarketData.MARKET_DATA.TICKER_CODE.eq(uuid))
                .fetchStreamInto(OHLCData.class)
                .toList();

        // Removed unused tickerInfo query
        // var tickerInfo = dsl.select(
        //         Tickers.TICKERS.TICKER,
        //         Tickers.TICKERS.TICKER_CODE
        // );

        return List.of();
    }
}
