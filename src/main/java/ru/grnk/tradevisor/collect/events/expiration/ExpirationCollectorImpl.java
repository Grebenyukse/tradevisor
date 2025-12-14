package ru.grnk.tradevisor.collect.events.expiration;

import com.google.protobuf.AbstractMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.events.expiration")
public class ExpirationCollectorImpl implements EventCollector {

    private final InvestApi investApi;

    @Override
    public List<String> collect(Tickers ticker) {
        if (!Objects.equals(ticker.getProvider(), "tinkoff")) return List.of();
        return investApi.getInstrumentsService()
                .getTradableFuturesSync()
                .stream()
                .filter(x -> x.getBasicAsset().equals(ticker.getTickerCode()))
                .findFirst()// сравнить по дате экспирации отфильтровать те, которые истекают в ближайшие 2 недели и выбрать ближайший фьюч
                .map(Future::getExpirationDate)
                .map(AbstractMessage::toString)
                .stream().toList();

    }
}
