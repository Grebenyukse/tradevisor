package ru.grnk.tradevisor.fnotify.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.zcommon.job.JobRunnerService;
import ru.grnk.tradevisor.zcommon.metrics.Jobs;
import ru.grnk.tradevisor.zcommon.repository.SignalsRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublishSignalsServiceImpl implements JobRunnerService {

    private final MessagePublisher messagePublisher;
    private final SignalsRepository signalsRepository;

    @Override
    public void doWork() {
        log.info("start signals publishing");
        signalsRepository.findUnpublishedSignals()
                .forEach(messagePublisher::publishMessage);
    }

    @Override
    public Jobs getJobType() {
        return Jobs.PUBLISH_SIGNAL_JOB;
    }
}
