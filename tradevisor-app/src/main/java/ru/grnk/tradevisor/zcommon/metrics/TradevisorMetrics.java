package ru.grnk.tradevisor.zcommon.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TradevisorMetrics {
    public static final String PREFIX = "ru.grnk.";
    public static final String JOB_TIMING_METRIC_NAME = PREFIX + "job.timing";
    public static final String JOB_TAG = "job";


    private final MeterRegistry registry;

    /**
     * тайминг работы какой либо задачи в рамках приклада с разбивкой по названию задачи
     */
    public Timer jobTiming(Jobs jobName) {
        return registry.timer(
            JOB_TIMING_METRIC_NAME,
            JOB_TAG,
            jobName.name()
        );
    }
}
