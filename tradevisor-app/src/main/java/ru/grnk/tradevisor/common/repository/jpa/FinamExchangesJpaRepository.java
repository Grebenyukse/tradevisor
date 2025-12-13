package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.FinamExchangesEntity;

public interface FinamExchangesJpaRepository extends JpaRepository<FinamExchangesEntity, Integer> {
}
