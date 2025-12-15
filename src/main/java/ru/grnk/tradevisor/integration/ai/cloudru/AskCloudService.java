package ru.grnk.tradevisor.integration.ai.cloudru;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.util.ObjectMapperUtils;
import ru.grnk.tradevisor.integration.ai.AskAiModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static chat.giga.http.client.HttpHeaders.CONTENT_TYPE;
import static chat.giga.http.client.MediaType.APPLICATION_JSON;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.integration.cloudru.enabled")
public class AskCloudService implements AskAiModel {

    private static final String API_URL_OPENAI = "https://foundation-models.api.cloud.ru/v1/chat/completions";
    private static final String MODEL_NAME_QWEN3 = "Qwen/Qwen3-Coder-480B-A35B-Instruct";
    private final TradevisorProperties tradevisorProperties;
    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public String ask(String prompt, Integer attempts) {
        String rqPrompt = StringEscapeUtils.escapeJava(prompt);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_OPENAI))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .header("authorization", "Bearer " + tradevisorProperties.integration().cloudru().key())
                .POST(HttpRequest.BodyPublishers.ofString(getBody(rqPrompt, MODEL_NAME_QWEN3)))
                .build();
        var res = sendRequest(request, HttpResponse.BodyHandlers.ofString());
        var content = ObjectMapperUtils.readValue(res.body(), ResContent.class);
        if (content == null) return "";
        return content.getChoices().stream().findFirst().get().getMessage().getContent()
                .replaceAll("[\\u00A0\\u202F]", "");
    }

    @SneakyThrows
    private static <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return httpClient.send(request, responseBodyHandler);
    }

    @Override
    public String source() {
        return "cloudru";
    }

    @Override
    public String basePrompt() {
        return prompt;
    }

    private static String getBody(String prompt, String model) {
        return """
                {
                    "model": "{model}",
                    "messages":[{"role":"user","content":"{prompt}"}],
                    "temperature": 0.7,
                    "max_tokens":15000,
                    "stream":false
                }
                """.replace("{model}", model)
                .replace("{prompt}", prompt);
    }

    private static final String prompt = """
            Act as a financial analyst. Based on a trading signal from my strategy, I need detailed information about a specific spot trading instrument to decide whether to buy or sell.
                        Please provide the following information **in Russian only**:
                        1. Brief description of the financial instrument:
                           - What does the issuer do?
                           - Which country does it operate in?
                           - What are the main factors affecting its price?
                        2. News background:
                           - Recent relevant news about the asset.
                           - Statements from key market participants (e.g., Twitter/X posts, interviews).
                           - Industry or government developments that could impact the price.
                        3. Lot size and cost in RUB:
                           - For spot trading: lot size and approximate cost in rubles.
                           - For futures (if available): same details.
                        4. Upcoming economic events:
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

    @Getter
    private static class AIMessage {
        String content;
    }

    @Getter
    private static class AIChoice {
        AIMessage message;
    }

    @Getter
    private static class  ResContent {
        List<AIChoice> choices;
    }
}
