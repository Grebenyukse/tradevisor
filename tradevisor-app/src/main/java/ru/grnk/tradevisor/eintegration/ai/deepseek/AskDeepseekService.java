package ru.grnk.tradevisor.eintegration.ai.deepseek;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.eintegration.ai.AskAiModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AskDeepseekService implements AskAiModel {
    private final RestTemplate restTemplate;

    @Value("${collect.ai.deepseek.url}")
    private String url;

    @Value("${collect.ai.deepseek.key}")
    private String key;

    @Override
    @SneakyThrows
    public String ask(String prompt, Integer attempts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", prompt
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
            return res.getBody();
        } catch (Exception e) {
            log.error("ошибка обращения к ai модели deepseek");
            if (attempts > 0) {
                Thread.sleep(1000L);
                return ask(prompt, --attempts);
            } else {
                throw e;
            }
        }
    }

}
