package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

public interface TickersJpa extends JpaRepository<Tickers, String> {
}
