package ru.grnk.tradevisor.job.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.job.JobRunnerService;
import ru.grnk.tradevisor.metrics.Jobs;
import ru.grnk.tradevisor.repository.TickersRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateSignalServiceImpl implements JobRunnerService {

    private final TickersRepository tickersRepository;

    @Override
    public void doWork() {
        log.info("calculate all signals mf");
        var tickers = tickersRepository.getAllTickers();
        if (tickers.isEmpty()) {
            log.info("нет тикеров ждемс когда появятся");
            return;
        }
        tickers.stream().forEach(x -> {

        });
    }

    @Override
    public Jobs getJobType() {
        return Jobs.CALCULATE_SIGNAL_JOB;
    }
}
