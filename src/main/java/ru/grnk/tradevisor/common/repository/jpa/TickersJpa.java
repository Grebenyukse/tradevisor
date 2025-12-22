package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.time.LocalDateTime;
import java.util.List;

public interface TickersJpa extends JpaRepository<Tickers, String> {

    @Query(value = "SELECT t FROM Tickers t WHERE t.provider = :provider AND t.marketType = :marketType AND t.spotTickerCode = :spotTickerCode")
    List<Tickers> findByProviderAndMarketTypeAndSpotTickerCode(@Param("provider") String provider,
                                                                @Param("marketType") String marketType,
                                                                @Param("spotTickerCode") String spotTickerCode);

    @Query(value = "SELECT t FROM Tickers t WHERE t.provider = :provider AND t.marketType = :marketType AND t.spotTickerCode IS NULL")
    List<Tickers> findByProviderAndMarketTypeAndSpotTickerCodeIsNull(@Param("provider") String provider,
                                                                      @Param("marketType") String marketType);



    @Modifying
    @Query(value = """
            INSERT INTO tradevisor.tickers (
                ticker_code,
                ticker,
                figi,
                description,
                market_type,
                exchange,
                precision,
                lot,
                go,
                expiration,
                currency,
                provider,
                status,
                load_priority,
                spot_ticker_code
            ) VALUES (
                ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15
            )
            ON CONFLICT (ticker_code)
            DO UPDATE SET
                ticker = EXCLUDED.ticker,
                figi = EXCLUDED.figi,
                description = EXCLUDED.description,
                market_type = EXCLUDED.market_type,
                exchange = EXCLUDED.exchange,
                precision = EXCLUDED.precision,
                lot = EXCLUDED.lot,
                go = EXCLUDED.go,
                expiration = EXCLUDED.expiration,
                currency = EXCLUDED.currency,
                provider = EXCLUDED.provider,
                status = EXCLUDED.status,
                load_priority = EXCLUDED.load_priority,
                spot_ticker_code = EXCLUDED.spot_ticker_code
            """, nativeQuery = true)
    void upsert(
            String tickerCode,
            String ticker,
            String figi,
            String description,
            String marketType,
            String exchange,
            Integer precision,
            Integer lot,
            Integer go,
            LocalDateTime expiration,
            String currency,
            String provider,
            String status,
            Integer loadPriority,
            String spotTickerCode
    );
}
