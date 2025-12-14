package ru.grnk.tradevisor.collect.events.economic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.ai.AskAiModel;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.events.economic")
public class EconomicEventCollectorImpl implements EventCollector {

    private final List<AskAiModel> loaders;

    public static final String TRV_CALENDAR_PROMPT = """
            Ты — эксперт в области фондового рынка. Мне требуется проанализировать текущий торговый сигнал и принять решение: покупать или продавать инструмент. Предоставь подробный анализ по следующим пунктам **на русском языке**:
            
             1. Общая информация об инструменте:
                - Деятельность эмитента.
                - География бизнеса.
                - Основные драйверы стоимости.

             2. Информация о фьючерсах:
                - Существуют ли фьючерсы на этот актив?
                - Какой фьючерс ближе всего к экспирации и активно торгуется?

             3. Анализ новостей:
                - Актуальные события вокруг компании.
                - Комментарии представителей бизнеса или регуляторов.
                - Отраслевые и государственные новости, способные повлиять на цену.

             4. Условия торговли:
                - Размер лота и его стоимость в рублях для спот-инструмента.
                - То же — для фьючерса, если он существует.

             5. Ключевые события в ближайшем будущем:
                - Даты выплат дивидендов.
                - Экспирации опционов или фьючерсов.
                - Возможные сплиты акций, программы выкупа и другие события.

             Данные для анализа:
             - Тикер: {tickername}
             - Биржа: {exchange}
             - Источник котировок: {provider}

             Обязательно отвечай на русском.
            """;

    @Override
    public List<String> collect(Tickers ticker) {
        return loaders.stream()
                .map(x -> x.ask(
                        ofNullable(x.basePrompt())
                                .orElse(TRV_CALENDAR_PROMPT)
                                .replace("{tickername}", ticker.getTicker())
                                .replace("{exchange}", ticker.getExchange())
                                .replace("{provider}", ticker.getProvider()), 3))
                .collect(Collectors.toList());
    }
}
