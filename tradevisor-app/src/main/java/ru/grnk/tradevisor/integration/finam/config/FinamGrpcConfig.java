package ru.grnk.tradevisor.integration.finam.config;


import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.marketdata.MarketDataServiceGrpc;
import grpc.tradeapi.v1.orders.OrdersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.finam.AuthService;

@Configuration
@RequiredArgsConstructor
public class FinamGrpcConfig {

    @Bean
    public ManagedChannel managedChannel(TradevisorProperties properties) {
        String host = properties.integration().finam().host();
        Integer port = properties.integration().finam().port();
        return ManagedChannelBuilder.forAddress(host, port).useTransportSecurity().build();
    }

    @Bean
    public AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub(ManagedChannel managedChannel) {
        return AccountsServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub(ManagedChannel managedChannel) {
        return AssetsServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub(ManagedChannel managedChannel) {
        return AuthServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub(ManagedChannel managedChannel) {
        return MarketDataServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub(ManagedChannel managedChannel) {
        return OrdersServiceGrpc.newBlockingStub(managedChannel);
    }




}
