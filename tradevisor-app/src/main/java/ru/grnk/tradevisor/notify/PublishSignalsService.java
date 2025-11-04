package ru.grnk.tradevisor.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.SignalsRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.notification.enabled")
public class PublishSignalsService {

    private final MessagePublisher messagePublisher;
    private final SignalsRepository signalsRepository;

    @Scheduled(cron = "${app.notification.cron}")
    public void doWork() {
        log.info("start signals publishing");
        signalsRepository.findUnpublishedSignals()
                .forEach(messagePublisher::publishMessage);
    }

}
