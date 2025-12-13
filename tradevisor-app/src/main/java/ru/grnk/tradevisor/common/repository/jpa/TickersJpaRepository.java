package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.TickersEntity;

public interface TickersJpaRepository extends JpaRepository<TickersEntity, String> {
}
