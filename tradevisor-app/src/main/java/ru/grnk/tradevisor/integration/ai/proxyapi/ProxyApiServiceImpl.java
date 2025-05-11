package ru.grnk.tradevisor.integration.ai.proxyapi;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.integration.ai.AskAiModel;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.proxyapi.enabled")
public class ProxyApiServiceImpl implements AskAiModel {
    private final RestTemplate restTemplate;
    private final TradevisorProperties trvProperties;

    @SneakyThrows
    @Override
    public String ask(String prompt, Integer attempts) {
        var url = trvProperties.integration().proxyapi().url();
        var key = trvProperties.integration().proxyapi().key();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "user",
                    "content", prompt
            ));
            requestBody.put("messages", messages);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            var res = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            if (res.getBody() == null) throw new IllegalStateException("proxy api returned no data");
            return res.getBody();
        } catch (Exception e) {
            log.error("ошибка обращения к ai модели proxyapi");
            if (attempts > 0) {
                Thread.sleep(1000L);
                return ask(prompt, --attempts);
            } else {
                throw e;
            }
        }
    }

    @Override
    public String source() {
        return "deepseek";
    }

}
