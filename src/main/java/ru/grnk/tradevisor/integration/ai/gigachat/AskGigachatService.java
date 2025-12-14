package ru.grnk.tradevisor.integration.ai.gigachat;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.integration.ai.AskAiModel;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.gigachat.enabled")
public class AskGigachatService implements AskAiModel {
    private static final String prompt = """
            You are a professional financial analyst. I have received a trade signal based on my strategy and now require comprehensive information about the underlying spot instrument to make a buy/sell decision.
            
            Provide your response **only in Russian** with the following structure:
            
            1. General overview of the instrument:
               - Business profile of the issuer.
               - Country of operations.
               - Key drivers influencing its value.
            
            2. Futures market status:
               - Presence of futures contracts.
               - Details of the nearest active futures contract if available.
            
            3. Current news sentiment:
               - Latest material events related to the company.
               - Public statements by executives or influencers.
               - Sectoral or governmental updates affecting the stock.
            
            4. Trading specifications:
               - Spot lot size and estimated cost in RUB.
               - Futures lot size and cost in RUB (if applicable).
            
            5. Scheduled events:
               - Dividend dates
               - Expirations of options/futures
               - Potential stock splits or buyback programs
               - Any other notable upcoming events
            
            Parameters:
            - Instrument: {tickername}
            - Exchange: {exchange}
            - Quote provider: {provider}
            
            Answer must be entirely in Russian.
            """;

    private final TradevisorProperties trvProperties;
    @SneakyThrows
    @Override
    public String ask(String prompt, Integer attempts) {
        var clientId = trvProperties.integration().gigachat().clientId();
        var clientSecret = trvProperties.integration().gigachat().clientSecret();
        GigaChatClient client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .clientId(clientId)
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey(clientSecret)
                                .build())
                        .build())
                .build();
        try {
            var res = client.completions(CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT_MAX)
                    .message(ChatMessage.builder()
                            .content(prompt)
                            .role(ChatMessageRole.USER)
                            .build())
                    .build());
            return res.choices().stream()
                    .findFirst()
                    .map(Choice::message)
                    .map(ChoiceMessage::content)
                    .orElseThrow();
        } catch (Exception e) {
            log.error("ошибка обращения к ai модели gigachat");
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
        return "gigachat";
    }

    @Override
    public String basePrompt() {
        return prompt;
    }
}
