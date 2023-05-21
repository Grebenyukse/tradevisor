package ru.grnk.tradevisor.job;

import ru.grnk.tradevisor.metrics.Jobs;

public interface JobRunnerService {

    void doWork();

    Jobs getJobType();
}
