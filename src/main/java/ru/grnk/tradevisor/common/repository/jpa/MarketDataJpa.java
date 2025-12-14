package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public interface MarketDataJpa extends JpaRepository<MarketData, MarketData.CompositeId> {

    @Modifying
    @Query(value = """
        INSERT INTO tradevisor.market_data (ticker_code, time, open, high, low, close)
        VALUES (?1, ?2, ?3, ?4, ?5, ?6)
        ON CONFLICT (ticker_code, time) DO NOTHING
        """, nativeQuery = true)
    void insertIgnore(String tickerCode, OffsetDateTime time, Float open, Float high, Float low, Float close);
}
