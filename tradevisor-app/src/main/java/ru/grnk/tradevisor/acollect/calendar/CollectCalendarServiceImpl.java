package ru.grnk.tradevisor.acollect.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.zcommon.job.JobRunnerService;
import ru.grnk.tradevisor.zcommon.metrics.Jobs;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectCalendarServiceImpl implements JobRunnerService {

    @Override
    public void doWork() {
        log.info("collect all calendars mf");
    }

    @Override
    public Jobs getJobType() {
        return Jobs.COLLECT_CALENDAR_JOB;
    }
}
