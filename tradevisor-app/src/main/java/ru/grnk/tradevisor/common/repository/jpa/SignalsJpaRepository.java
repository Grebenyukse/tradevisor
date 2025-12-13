package ru.grnk.tradevisor.common.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grnk.tradevisor.common.repository.entity.SignalsEntity;

import java.time.OffsetDateTime;
import java.util.List;

public interface SignalsJpaRepository extends JpaRepository<SignalsEntity, Integer> {
    List<SignalsEntity> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
    List<SignalsEntity> findByStatusEqualsOrderByCreatedAtAsc(String status);
    List<SignalsEntity> findExpiredSignals(List<String> statuses, OffsetDateTime cutoffTime);
}
