package ru.grnk.tradevisor.acollect.calendar.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.acollect.aiscore.AiScoreRepository;
import ru.grnk.tradevisor.acollect.calendar.CalendarCollector;
import ru.grnk.tradevisor.acollect.EconomicEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.acollect.calendar.ai.TrvCalendarPrompt.TRV_CALENDAR_PROMPT;
import static ru.grnk.tradevisor.zcommon.util.ObjectIdHasher.calcHash;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "calendar.ai.proxyapi.enabled")
public class ProxyApiServiceImpl implements CalendarCollector {
    private final RestTemplate restTemplate;
    private final AiScoreRepository repository;

    @Value("${calendar.ai.proxyapi.url}")
    private String url;

    @Value("${calendar.ai.proxyapi.key}")
    private String key;

    @Override
    public List<EconomicEvent> collect() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "user",
                    "content", TRV_CALENDAR_PROMPT
            ));
            requestBody.put("messages", messages);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            var res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AiCalendarResponse[].class
            );
            if (res.getBody() == null) return List.of();
            return Arrays.stream(res.getBody())
                    .flatMap(x -> x.tickers().stream()
                            .map(y -> EconomicEvent.builder()
                                    .id(calcHash(x.title(), x.description(), x.event_date(), y, x.impact()))
                                    .eventDate(LocalDateTime.parse(x.event_date()))
                                    .category("economic event")
                                    .impact(x.impact())
                                    .country(x.country())
                                    .source("investing")
                                    .ticker(y)
                                    .title(x.title())
                                    .description(x.description())
                                    .build())
                    )
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    @Override
    public String source() {
        return "deepseek";
    }

}
