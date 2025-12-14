package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;

public interface MarketDataJpa extends JpaRepository<MarketData, MarketData.CompositeId> {
}
