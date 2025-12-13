package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketDataEntity;

public interface MarketDataJpaRepository extends JpaRepository<MarketDataEntity, MarketDataEntity.CompositeId> {
}
