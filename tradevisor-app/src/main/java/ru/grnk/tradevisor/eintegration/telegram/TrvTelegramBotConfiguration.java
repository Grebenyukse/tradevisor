package ru.grnk.tradevisor.eintegration.telegram;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.zcommon.properties.TradevisorProperties;

@Configuration
public class TrvTelegramBotConfiguration {

    @Bean
    public TrvTelegramBot trvTelegramBot(TelegramFacade telegramFacade, TradevisorProperties tradevisorProperties) {
        var chatToken = tradevisorProperties.integration().telegram().chatToken();
        return new TrvTelegramBot(
                chatToken,
                telegramFacade
        );
    }
}
