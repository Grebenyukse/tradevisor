package ru.grnk.tradevisor.job.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectPricesServiceImpl implements JobRunnerService {

    @Override
    public void doWork() {
        log.info("collect all prices mf");
    }

    @Override
    public Jobs getJobType() {
        return Jobs.COLLECT_PRICES_JOB;
    }
}
