package ru.grnk.tradevisor.acollect.aiscore;

import ru.grnk.tradevisor.acollect.TrvCollector;

public interface AiScoreCollector extends TrvCollector<AiScoreResponse> {
     String TRV_AI_PROMPT = """
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


}
