package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LastTickLoader {
    public final List<PricesLoader> loaders;

    public float getLastCloseForTicker(String tickerCode, String provider) {
        return loaders.stream().filter(l -> Objects.equals(l.getProvider(), provider))
                .findFirst()
                .orElseThrow()
                .getBidForTicker(tickerCode);
    }
}
