package ru.grnk.tradevisor.acollect.aiscore.impl;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.acollect.aiscore.AiScoreCollector;
import ru.grnk.tradevisor.acollect.aiscore.AiScoreResponse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.zcommon.util.ObjectMapperUtils.readValue;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "collect.ai.gigachat.enabled")
public class GigachatServiceImpl implements AiScoreCollector {

    @Value("${collect.ai.gigachat.client-id}")
    private String clientId;

    @Value("${collect.ai.gigachat.client-secret}")
    private String clientSecret;

    @Value("${collect.ai.gigachat.url}")
    private String url;

    @Override
    public List<AiScoreResponse> collect() {
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
                            .content(TRV_AI_PROMPT)
                            .role(ChatMessageRole.USER)
                            .build())
                    .build());
            return res.choices().stream().map(Choice::message)
                    .map(ChoiceMessage::content)
                    .map(x -> readValue(x, AiScoreResponse[].class))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("ошибка обращения к gigachat", e);
            throw e;
        }
    }

    @Override
    public String source() {
        return "gigachat";
    }

}
