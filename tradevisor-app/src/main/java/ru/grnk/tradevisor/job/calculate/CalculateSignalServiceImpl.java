package ru.grnk.tradevisor.job.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateSignalServiceImpl implements JobRunnerService {

    @Override
    public void doWork() {
        log.info("calculate all signals mf");
    }

    @Override
    public Jobs getJobType() {
        return Jobs.CALCULATE_SIGNAL_JOB;
    }
}
