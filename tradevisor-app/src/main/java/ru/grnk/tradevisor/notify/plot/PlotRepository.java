package ru.grnk.tradevisor.notify.plot;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.Tickers;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PlotRepository {

    private final DSLContext dsl;

    public List<PlotRecord> getTickerPlotInfo(String uuid) {
        return dsl.select(
                MarketData.MARKET_DATA.OPEN,
                MarketData.MARKET_DATA.HIGH,
                MarketData.MARKET_DATA.LOW,
                MarketData.MARKET_DATA.CLOSE,
                Tickers.TICKERS.TICKER,
                Tickers.TICKERS.FIGI,
                Tickers.TICKERS.UUID,
                MarketData.MARKET_DATA.TIME
                ).from(Tickers.TICKERS.join(MarketData.MARKET_DATA)
                .on(Tickers.TICKERS.UUID.eq(MarketData.MARKET_DATA.INSTRUMENT_UUID)))
                .where(Tickers.TICKERS.UUID.eq(uuid))
                .fetchStreamInto(PlotRecord.class)
                .collect(Collectors.toList());
    }
}
