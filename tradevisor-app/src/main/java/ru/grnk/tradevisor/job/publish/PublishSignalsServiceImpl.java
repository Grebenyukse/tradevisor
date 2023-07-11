package ru.grnk.tradevisor.job.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;
import ru.grnk.tradevisor.repository.SignalsRepository;
import ru.grnk.tradevisor.telegram.BotMessage;
import ru.grnk.tradevisor.telegram.out.BotMsgSender;

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
