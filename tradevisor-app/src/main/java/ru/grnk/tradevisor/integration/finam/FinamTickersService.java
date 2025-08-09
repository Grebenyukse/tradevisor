package ru.grnk.tradevisor.integration.finam;


import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jvnet.hk2.annotations.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.integration.finam.repository.FinamMetainfoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinamTickersService {
    private final TradevisorProperties properties;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final FinamMetainfoRepository finamMetainfoRepository;

    public void initTickers() {
        TrvFinamProperties finamProperties = properties.integration().finam();

    }
}
