package ru.grnk.tradevisor.common.repository.jpa;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.grnk.tradevisor.common.repository.entity.Signals;

import java.time.OffsetDateTime;
import java.util.List;

public interface SignalsJpa extends JpaRepository<Signals, Integer> {
    List<Signals> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
    List<Signals> findByStatusEqualsOrderByCreatedAtAsc(String status);

    @Modifying
    @Transactional
    @Query("UPDATE Signals s SET s.status = :newStatus, s.updatedAt = :updatedAt " +
            "WHERE s.status IN :statuses AND s.createdAt < :cutoffTime")
    int expirePublishedSignals(@Param("statuses") List<String> statuses,
                               @Param("cutoffTime") OffsetDateTime cutoffTime,
                               @Param("updatedAt") OffsetDateTime updatedAt,
                               @Param("newStatus") String newStatus);

}
