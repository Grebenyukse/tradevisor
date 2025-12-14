package ru.grnk.tradevisor.integration.ai.deepseek;

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
@ConditionalOnProperty(value = "app.integration.deepseek.enabled")
public class AskDeepseekService implements AskAiModel {

    public static String prompt = """
                        Act as a financial analyst. Based on a trading signal from my strategy, I need detailed information about a specific spot trading instrument to decide whether to buy or sell.
            
                        Please provide the following information **in Russian only**:
            
                        1. Brief description of the financial instrument:
                           - What does the issuer do?
                           - Which country does it operate in?
                           - What are the main factors affecting its price?
            
                        2. Futures availability:
                           - Are there any futures contracts for this instrument?
                           - If so, specify the nearest actively traded futures contract.
            
                        3. News background:
                           - Recent relevant news about the asset.
                           - Statements from key market participants (e.g., Twitter/X posts, interviews).
                           - Industry or government developments that could impact the price.
            
                        4. Lot size and cost in RUB:
                           - For spot trading: lot size and approximate cost in rubles.
                           - For futures (if available): same details.
            
                        5. Upcoming economic events:
                           - Dividend payments
                           - Contract expirations
                           - Stock splits
                           - Share buybacks
                           - Other major corporate or market events
            
                        Input parameters:
                        - Ticker: {tickername}
                        - Exchange: {exchange}
                        - Data Provider: {provider}
            
                        Respond strictly in Russian.
            """;

    private final RestTemplate restTemplate;
    private final TradevisorProperties trvProperties;

    @Override
    @SneakyThrows
    public String ask(String prompt, Integer attempts) {
        var url = trvProperties.integration().deepseek().url();
        var key = trvProperties.integration().deepseek().key();
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

    @Override
    public String source() {
        return "deepseek";
    }

    @Override
    public String basePrompt() {
        return prompt;
    }

}
