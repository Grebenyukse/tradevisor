package ru.grnk.tradevisor.zcommon.job;

import lombok.extern.slf4j.Slf4j;
import ru.grnk.tradevisor.zcommon.metrics.Jobs;
import ru.grnk.tradevisor.zcommon.metrics.TradevisorMetrics;

import java.util.List;

@Slf4j
public class JobRunner implements Runnable {
    private final TradevisorMetrics metrics;
    private final List<JobRunnerService> services;
    private final Jobs jobType;

    public JobRunner(TradevisorMetrics metrics,
                     List<JobRunnerService> services,
                     Jobs jobType) {
        this.metrics = metrics;
        this.services = services;
        this.jobType = jobType;
    }

    @Override
    public void run() {
        metrics.jobTiming(jobType)
                .record(() -> {
                    log.debug("{} запущен", this.getClass().getSimpleName());
                    try {
                        services.stream()
                                .filter(x -> x.getJobType() == jobType)
                                .findFirst()
                                .orElseThrow()
                                .doWork();
                    } catch (Exception e) {
                        log.error("ошибка исполнения", e);
                    } finally {
                        log.debug("{} завершён", this.getClass().getSimpleName());
                    }
                });
    }
}
