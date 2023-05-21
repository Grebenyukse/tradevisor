package ru.grnk.tradevisor.job.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublishSignalsServiceImpl implements JobRunnerService {

    @Override
    public void doWork() {
        log.info("publish all signals mf");
    }

    @Override
    public Jobs getJobType() {
        return Jobs.PUBLISH_SIGNAL_JOB;
    }
}
