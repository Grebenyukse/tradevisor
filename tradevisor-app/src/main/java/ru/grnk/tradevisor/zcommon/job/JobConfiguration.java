package ru.grnk.tradevisor.zcommon.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.grnk.tradevisor.zcommon.metrics.Jobs;
import ru.grnk.tradevisor.zcommon.metrics.TradevisorMetrics;
import ru.grnk.tradevisor.zcommon.properties.jobs.CalculateJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.CalendarJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.ControlJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.NewsJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.PricesJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.PublishJobProperties;

import java.util.List;

@EnableScheduling
@Slf4j
@Configuration
public class JobConfiguration {

    @Bean
    public JobRunner calculateSignalsJob(
            TaskScheduler taskScheduler,
            CalculateJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
    ) {
        var job = new JobRunner(metrics, services, Jobs.CALCULATE_SIGNAL_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.CALCULATE_SIGNAL_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }

    @Bean
    public JobRunner collectCalendarJob(
            TaskScheduler taskScheduler,
            CalendarJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
    ) {
        var job = new JobRunner(metrics, services, Jobs.COLLECT_CALENDAR_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.COLLECT_CALENDAR_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }

    @Bean
    public JobRunner controlPositionJob(
            TaskScheduler taskScheduler,
            ControlJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
    ) {
        var job = new JobRunner(metrics, services, Jobs.CONTROL_POSITION_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.CONTROL_POSITION_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }

    @Bean
    public JobRunner collectNewsJob(
            TaskScheduler taskScheduler,
            NewsJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
    ) {
        var job = new JobRunner(metrics, services, Jobs.COLLECT_NEWS_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.COLLECT_NEWS_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }

    @Bean
    public JobRunner collectPricesJob(
            TaskScheduler taskScheduler,
            PricesJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
            ) {
        var job = new JobRunner(metrics, services, Jobs.COLLECT_PRICES_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.COLLECT_PRICES_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }



    @Bean
    public JobRunner publishSignalsJob(
            TaskScheduler taskScheduler,
            PublishJobProperties properties,
            TradevisorMetrics metrics,
            List<JobRunnerService> services
    ) {
        var job = new JobRunner(metrics, services, Jobs.PUBLISH_SIGNAL_JOB);
        if (properties.enabled()) {
            log.info("Scheduling {} with delay: {}", Jobs.PUBLISH_SIGNAL_JOB.name(), properties.interval());
            taskScheduler.scheduleWithFixedDelay(job, properties.interval());
        }
        return job;
    }

}
