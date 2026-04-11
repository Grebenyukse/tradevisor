package ru.grnk.tradevisor.common.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.jpa.SignalsJpa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SignalsRepository {

    private final SignalsJpa signalsRepo;

    @Transactional
    public void updateSignalStatus(Integer signalId, TrvSignalStatus status) {
        Optional<Signals> opt = signalsRepo.findById(signalId);
        if (opt.isPresent()) {
            Signals entity = opt.get();
            entity.setStatus(status.name());
            entity.setUpdatedAt(OffsetDateTime.now());
            signalsRepo.saveAndFlush(entity);
        }
    }

    @Transactional
    public List<Signals> findSignalsByStatuses(List<String> statuses) {
        return signalsRepo.findByStatusInOrderByCreatedAtAsc(statuses);
    }

    @Transactional
    public void cancelExpiredSignals(List<Integer> ids) {
        List<Signals> entities = signalsRepo.findAllById(ids);
        entities.forEach(e -> {
            e.setStatus(TrvSignalStatus.CANCELLED.name());
            e.setUpdatedAt(OffsetDateTime.now());
        });
        signalsRepo.saveAll(entities);
    }

    @Transactional
    public List<Signals> findUnpublishedSignals() {
        return signalsRepo.findByStatusEqualsOrderByCreatedAtAsc(TrvSignalStatus.CREATED.name());
    }

    public Optional<Signals> findSignalBySignalId(Integer id) {
        return signalsRepo.findById(id);
    }

    @Transactional
    public List<Signals> findPublishedSignals() {
        return signalsRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(TrvSignalStatus.CREATED.name(), TrvSignalStatus.PUBLISHED.name())
        );
    }

    @Transactional
    public int expirePublishedSignals(int retentionDays) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);
        return signalsRepo.expirePublishedSignals(List.of(TrvSignalStatus.PUBLISHED.name(), TrvSignalStatus.CANCELLED.name()),
                cutoffTime,
                OffsetDateTime.now(),
                TrvSignalStatus.EXPIRED.name()
        );
    }

    @Transactional
    public List<Signals> findAcceptedSignals() {
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
        Signals entity = Signals.builder()
                .tickerCode(tickerCode)
                .name(strategyName)
                .direction(trvCalculationResult.direction().directionCode())
                .priceOpen(trvCalculationResult.priceOpen())
                .stopLoss(trvCalculationResult.stopLoss())
                .takeProfit(trvCalculationResult.takeProfit())
                .description(signalDescription)
                .status(TrvSignalStatus.CREATED.name())
                .createdAt(lastCandleTime)
                .strategyProps(trvCalculationResult.lines())
                .build();
        signalsRepo.saveAndFlush(entity);
    }

    @Transactional
    public void saveSignal(Signals signal) {
        signalsRepo.saveAndFlush(signal);
    }

    @Transactional
    public void deleteAllSignals() {
        signalsRepo.deleteAll();
    }
}
