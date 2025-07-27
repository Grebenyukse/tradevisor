package ru.grnk.tradevisor.integration.finam;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;

@Configuration
public class FinamClientConfiguration {

    @Value("${finam.api.host:trade-api.finam.ru}")
    private String host;

    @Value("${finam.api.port:443}")
    private int port;

    @Value("${finam.api.token}")
    private String apiToken;

    @Bean
    public ManagedChannel finamChannel(TrvFinamProperties properties) {

        return ManagedChannelBuilder.forAddress(host, port)
                .useTransportSecurity()
                .build();
    }

    @Bean
    public Metadata authHeaders() {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("X-API-TOKEN", Metadata.ASCII_STRING_MARSHALLER), apiToken);
        return headers;
    }
}
