package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.FinamExchanges;

public interface FinamExchangesJpa extends JpaRepository<FinamExchanges, Integer> {
}
