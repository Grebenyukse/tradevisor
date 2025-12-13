package ru.grnk.tradevisor.common.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.entity.SignalsEntity;
import ru.grnk.tradevisor.common.repository.jpa.SignalsJpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SignalsRepository {

    private final SignalsJpaRepository signalsRepo;
    private final ObjectMapper om;

    @Transactional
    public void updateSignalStatus(Integer signalId, TrvSignalStatus status) {
        Optional<SignalsEntity> opt = signalsRepo.findById(signalId);
        if (opt.isPresent()) {
            SignalsEntity entity = opt.get();
            entity.setStatus(status.name());
            entity.setUpdatedAt(OffsetDateTime.now());
            signalsRepo.save(entity);
        }
    }

    @Transactional
    public List<SignalsEntity> findSignalsByStatuses(List<String> statuses) {
        return signalsRepo.findByStatusInOrderByCreatedAtAsc(statuses);
    }

    @Transactional
    public void cancelExpiredSignals(List<Integer> ids) {
        List<SignalsEntity> entities = signalsRepo.findAllById(ids);
        entities.forEach(e -> {
            e.setStatus(TrvSignalStatus.CANCELLED.name());
            e.setUpdatedAt(OffsetDateTime.now());
        });
        signalsRepo.saveAll(entities);
    }

    @Transactional
    public List<SignalsEntity> findUnpublishedSignals() {
        return signalsRepo.findByStatusEqualsOrderByCreatedAtAsc(TrvSignalStatus.CREATED.name());
    }

    public Optional<SignalsEntity> findSignalBySignalId(Integer id) {
        return signalsRepo.findById(id);
    }

    @Transactional
    public List<SignalsEntity> findPublishedSignals() {
        return signalsRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(TrvSignalStatus.CREATED.name(), TrvSignalStatus.PUBLISHED.name())
        );
    }

    @Transactional
    public int expirePublishedSignals(int retentionDays) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);
        List<SignalsEntity> expired = signalsRepo.findExpiredSignals(
                List.of(TrvSignalStatus.PUBLISHED.name(), TrvSignalStatus.CANCELLED.name()),
                cutoffTime
        );

        expired.forEach(e -> {
            e.setStatus(TrvSignalStatus.EXPIRED.name());
            e.setUpdatedAt(OffsetDateTime.now());
        });

        signalsRepo.saveAll(expired);
        return expired.size();
    }

    @Transactional
    public List<SignalsEntity> findAcceptedSignals() {
        return signalsRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(TrvSignalStatus.CONFIRMED.name())
        );
    }

    @SneakyThrows
    @Transactional
    public void saveSignal(TrvCalculationResult trvCalculationResult,
                           String tickerCode,
                           String strategyName,
                           OffsetDateTime lastCandleTime,
                           String signalDescription
    ) {
        SignalsEntity entity = new SignalsEntity();
        entity.setTickerCode(tickerCode);
        entity.setName(strategyName);
        entity.setDirection(trvCalculationResult.direction().directionCode());
        entity.setPriceOpen(trvCalculationResult.priceOpen());
        entity.setStopLoss(trvCalculationResult.stopLoss());
        entity.setTakeProfit(trvCalculationResult.takeProfit());
        entity.setDescription(signalDescription);
        entity.setStatus(TrvSignalStatus.CREATED.name());
        entity.setCreatedAt(lastCandleTime);
        entity.setStrategyProps(om.writeValueAsString(trvCalculationResult.lines()));

        signalsRepo.save(entity);
    }

    @Transactional
    public void deleteAllSignals() {
        signalsRepo.deleteAll();
    }
}
