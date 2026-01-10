package ru.grnk.tradevisor.integration.bybit;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.restApi.BybitApiAssetRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

@Configuration
public class BybitConfiguration {

    @Bean
    public BybitApiClientFactory bybitApiClientFactory(TradevisorProperties tradevisorProperties) {
        var apiKey = tradevisorProperties.integration().bybit().key();
        var apiSecret = tradevisorProperties.integration().bybit().secret();
        var baseUrl = tradevisorProperties.integration().bybit().url();
        String domain = baseUrl.contains("testnet") ? BybitApiConfig.TESTNET_DOMAIN : BybitApiConfig.MAINNET_DOMAIN;
        return BybitApiClientFactory.newInstance(apiKey, apiSecret, domain, 20000L);
    }

    @Bean
    public BybitApiTradeRestClient bybitApiTradeRestClient(BybitApiClientFactory factory) {
        return factory.newTradeRestClient();
    }

    @Bean
    public BybitApiAssetRestClient bybitApiAssetRestClient(BybitApiClientFactory factory) {
        return factory.newAssetRestClient();
    }

    @Bean
    public BybitApiMarketRestClient bybitApiMarketRestClient(BybitApiClientFactory factory) {
        return factory.newMarketDataRestClient();
    }
}
