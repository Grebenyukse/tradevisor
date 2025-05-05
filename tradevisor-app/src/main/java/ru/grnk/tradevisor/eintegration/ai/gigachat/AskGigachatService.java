package ru.grnk.tradevisor.eintegration.ai.gigachat;

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
import ru.grnk.tradevisor.eintegration.ai.AskAiModel;
import ru.grnk.tradevisor.zcommon.properties.TradevisorProperties;

import static ru.grnk.tradevisor.zcommon.util.ObjectMapperUtils.readValue;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.integration.gigachat.enabled")
public class AskGigachatService implements AskAiModel {
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
}
