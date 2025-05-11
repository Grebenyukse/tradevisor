package ru.grnk.tradevisor.notify.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.SignalsRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublishSignalsServiceImpl {

    private final MessagePublisher messagePublisher;
    private final SignalsRepository signalsRepository;

    public void doWork() {
        log.info("start signals publishing");
        signalsRepository.findUnpublishedSignals()
                .forEach(messagePublisher::publishMessage);
    }

}
