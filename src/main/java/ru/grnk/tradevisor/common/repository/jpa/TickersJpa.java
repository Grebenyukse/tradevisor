package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.time.LocalDateTime;
import java.util.List;

public interface TickersJpa extends JpaRepository<Tickers, String> {

    @Query(value = "SELECT t FROM Tickers t WHERE t.provider = :provider AND t.spotTickerCode = :spotTickerCode")
    List<Tickers> findByProviderAndSpotTickerCode(@Param("provider") String provider,
                                                   @Param("spotTickerCode") String spotTickerCode);

    @Query(value = "SELECT t FROM Tickers t WHERE t.provider = :provider AND t.spotTickerCode IS NULL")
    List<Tickers> findByProviderAndSpotTickerCodeIsNull(@Param("provider") String provider);



    @Modifying
    @Query(value = """
            INSERT INTO tradevisor.tickers (
                ticker_code,
                ticker,
                description,
                exchange,
                currency,
                provider,
                status,
                version,
                spot_ticker_code
            ) VALUES (
                ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9
            )
            ON CONFLICT (ticker_code)
            DO UPDATE SET
                ticker = EXCLUDED.ticker,
                description = EXCLUDED.description,
                exchange = EXCLUDED.exchange,
                currency = EXCLUDED.currency,
                provider = EXCLUDED.provider,
                status = EXCLUDED.status,
                version = EXCLUDED.version,
                spot_ticker_code = EXCLUDED.spot_ticker_code
            """, nativeQuery = true)
    void upsert(
            String tickerCode,
            String ticker,
            String description,
            String exchange,
            String currency,
            String provider,
            String status,
            Integer version,
            String spotTickerCode
    );


    @Modifying
    @Query(value = """
    INSERT INTO tradevisor.tickers (
        ticker_code,
        ticker,
        description,
        exchange,
        currency,
        provider,
        status,
        version,
        spot_ticker_code
    )
    SELECT
        :tickerCode,
        :ticker,
        :description,
        :exchange,
        :currency,
        :provider,
        :status,
        :version,
        :spotTickerCode
    WHERE NOT EXISTS (
        SELECT 1 FROM tradevisor.tickers t
        WHERE t.ticker = :ticker AND t.provider = ':excludeProvider'
    )
""", nativeQuery = true)
    int insertIfTickerAndProviderNotExist(
            @Param("tickerCode") String tickerCode,
            @Param("ticker") String ticker,
            @Param("description") String description,
            @Param("exchange") String exchange,
            @Param("currency") String currency,
            @Param("provider") String provider,
            @Param("status") String status,
            @Param("version") Integer version,
            @Param("spotTickerCode") String spotTickerCode,
            @Param("excludeProvider") String excludeProvider);
}
