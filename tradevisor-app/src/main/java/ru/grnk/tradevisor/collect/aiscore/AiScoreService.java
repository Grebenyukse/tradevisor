package ru.grnk.tradevisor.collect.aiscore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.aiscore.dto.AiScoreEntity;
import ru.grnk.tradevisor.collect.aiscore.dto.AiScoreResponse;
import ru.grnk.tradevisor.integration.ai.AskAiModel;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.common.util.ObjectMapperUtils.readValue;
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.aiscore.enabled")
public class AiScoreService {
    private final static String TRV_AI_PROMPT = """
            Сформируй строго в формате JSON массив объектов со следующими полями:
            - ticker: международный тикер инструмента
            - score: сила сигнала от -100 (максимальная продажа) до 100 (максимальная покупка)
                        
            Инструменты для анализа: [BTC/USD, ETH/USD, USD/RUB, EUR/USD] + голубые фишки ММВБ.\s
                        
            Требования:
            1. Только валидный JSON без комментариев
            2. Никаких дополнительных пояснений или текста
            3. Формат ответа:
            [
              {"ticker": "XXX", "score": N},
              ...
            ]
            4. Оценивай текущие рыночные условия, дивидендные ожидания и макрофакторы
                        
            Верни только готовый к парсингу JSON-массив без форматирования.
            """;
    private final List<AskAiModel> aiScoreCollectors;
    private final AiScoreRepository aiScoreRepository;

    @Scheduled(cron = "${app.collect.aiscore.cron}")
    public void doWork() {
        var date = java.sql.Date.valueOf(LocalDate.now());
        try {
            var aiScores = aiScoreCollectors.stream()
                    .map(x -> Pair.of(x.ask(TRV_AI_PROMPT, 3), x.source()))
                    .flatMap(c -> Arrays.stream(readValue(c.getLeft(), AiScoreResponse[].class))
                            .map(x -> new AiScoreEntity(date, x.score(), x.ticker(), c.getRight()))
                    )
                    .collect(Collectors.toList());
            aiScoreRepository.saveAiScore(aiScores);
        } catch (Exception e) {
            log.error("ошибка скоринга ", e);
        }
    };

}
