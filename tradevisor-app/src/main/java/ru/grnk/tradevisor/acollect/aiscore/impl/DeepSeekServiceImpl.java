package ru.grnk.tradevisor.acollect.aiscore.impl;

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
import ru.grnk.tradevisor.acollect.aiscore.AiScoreCollector;
import ru.grnk.tradevisor.acollect.aiscore.AiScoreRepository;
import ru.grnk.tradevisor.acollect.aiscore.AiScoreResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.grnk.tradevisor.acollect.calendar.ai.TrvCalendarPrompt.TRV_CALENDAR_PROMPT;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "collect.ai.deepseek.enabled")
public class DeepSeekServiceImpl implements AiScoreCollector {
    private final RestTemplate restTemplate;
    private final AiScoreRepository repository;

    @Value("${collect.ai.deepseek.url}")
    private String url;

    @Value("${collect.ai.deepseek.key}")
    private String key;

    @Override
    public List<AiScoreResponse> collect() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", TRV_CALENDAR_PROMPT
            ));
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            var res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    @Override
    public String source() {
        return "deepseek";
    }

    private List<AiScoreResponse> parseResponse(String json) {
        // Реализация парсинга с Jackson
        return List.of();
    }
}
