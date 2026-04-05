package ru.grnk.tradevisor.cleanup;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.log.CleanUpLogsService;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.SignalsRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.cleanup.enabled")
public class SignalCleanupService {

    private final SignalsRepository signalsRepository;
    private final TradevisorProperties tradevisorProperties;
    private final CleanUpLogsService cleanUpLogsService;

    @Scheduled(cron = "${app.cleanup.cron}")
    public void cleanup() {
        log.info("cleanup service start");
        var retention = tradevisorProperties.cleanup().retentionDays();
        var res = signalsRepository.expirePublishedSignals(retention);
        log.info("переведено в статус EXPIRED {} сигналов. retention: {}", res, retention);
        cleanUpLogsService.deleteByLogDateBefore(LocalDateTime.now().minusDays(30));
    }
}
