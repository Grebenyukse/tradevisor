package ru.grnk.tradevisor.integration.tinkoff;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.tinkoff.piapi.core.InvestApi;


@Configuration
public class TInkoffAccountConfig {

    @Bean
    public InvestApi investApi(TradevisorProperties trvProperties) {
        var token = trvProperties.integration().tinkoff().token();
        return InvestApi.create(token);
    }
}
