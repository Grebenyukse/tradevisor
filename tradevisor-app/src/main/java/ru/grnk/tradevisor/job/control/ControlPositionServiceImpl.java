package ru.grnk.tradevisor.job.control;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;

@Service
@Slf4j
@RequiredArgsConstructor
public class ControlPositionServiceImpl implements JobRunnerService {

    @Override
    public void doWork() {
        log.info("control all positions mf");
    }

    @Override
    public Jobs getJobType() {
        return Jobs.CONTROL_POSITION_JOB;
    }
}
