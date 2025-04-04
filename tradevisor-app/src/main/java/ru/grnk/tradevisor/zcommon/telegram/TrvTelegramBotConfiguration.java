package ru.grnk.tradevisor.zcommon.telegram;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrvTelegramBotConfiguration {

    @Value("${telegram.chat-token}")
    private String chatToken;

    @Bean
    public TrvTelegramBot trvTelegramBot(TelegramFacade telegramFacade) {
        return new TrvTelegramBot(
                chatToken,
                telegramFacade
        );
    }
}
