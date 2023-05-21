package ru.grnk.tradevisor.zcommon.tinkoff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tinkoff.piapi.core.InvestApi;


@Configuration
public class TInkoffAccountConfig {

    @Value("${tinkoff.token}")
    private String token;


    @Bean
    public InvestApi investApi() {
        return InvestApi.create(token);
    }
}
