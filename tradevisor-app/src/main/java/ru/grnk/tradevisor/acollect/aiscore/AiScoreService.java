package ru.grnk.tradevisor.acollect.aiscore;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiScoreService {
    private final List<AiScoreCollector> aiScoreCollectors;
    private final AiScoreRepository aiScoreRepository;

    @Scheduled(cron = "${collect.ai.gigachat.cron}")
    public void doWork() {
        var date = java.sql.Date.valueOf(LocalDate.now());
        var aiScores = aiScoreCollectors.stream()
                .flatMap(c -> c.collect()
                        .stream()
                        .map(x -> new AiScoreEntity(date, x.score(), x.ticker(), c.source()))
                )
                .collect(Collectors.toList());
        aiScoreRepository.saveAiScore(aiScores);
    };

}
