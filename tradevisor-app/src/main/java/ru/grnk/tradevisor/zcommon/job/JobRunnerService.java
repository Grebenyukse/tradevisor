package ru.grnk.tradevisor.zcommon.job;

import ru.grnk.tradevisor.zcommon.metrics.Jobs;

public interface JobRunnerService {

    void doWork();

    Jobs getJobType();
}
