package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.grnk.tradevisor.collect.prices.Futures2SpotMap.*;

/**
 * 1. загружаются все активные фьючерсы с тинькоф API
 * 2. по каждому фьючу определяется привязанный спот-тикер
 * 3. фьючерсы размечаются связкой tickerCode-spotTickerCode
 * 4. при открытии позиции проверяется, есть ли привязанный futures. Если да - выставляется позиция по нему. если нет -
 * торговля спотом.
 * 5. фьючерсы у которых нет явной привязке к споту, но при этом контракт широко используется (например Brent)
 * склеиваются в бесконечный фьючерс для проведения технического анализа по стратегиям.
 * бесконечный фьючерс получается по первым двум буквам tickerCode фьючерса, но вместо кода экспирации (H6, M6, U6, Z6) ->
 * указываетя хардкод "!1" как в TradingView.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BindTradeFuturesService {

    public static final String TRV_FUTURES_ASSET_TYPE = "futures";
    public static final String TRV_PROVIDER_TINKOFF = "tinkoff";
    private final InvestApi investApi;
    private final TickersRepository tickersRepository;

    @SneakyThrows
    void initTickers() {
        investApi.getInstrumentsService().getTradableFuturesSync()
                .forEach(future -> {
                    if (future == null) return;
                    future.getBasicAssetPositionUid();
                    if (StringUtils.isEmpty(future.getBasicAssetPositionUid())) {
                        saveFutureWithoutLink(future);
                        return;
                    }
                    while(true) {
                        try {
                            investApi.getInstrumentsService()
                                    .findInstrumentSync(future.getBasicAssetPositionUid())
                                    .stream()
                                    .findFirst()
                                    .ifPresentOrElse((spotTicker) -> saveFutureWithSpotLink(future, spotTicker), () -> saveFutureWithoutLink(future));
                            return;
                        } catch (Exception e) {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                });
        tickersRepository.findUnlinkedFutures(TRV_PROVIDER_TINKOFF)
                .stream()
                .map(this::getTickerSearchPart2FutureTickerCode)
                .map(x -> Pair.of(FUTURES_2_SPOT.get(x.getLeft()), x.getRight()))
                .filter(x -> x.getLeft() != null)
                .filter(x -> !x.getLeft().equals(SKIP_TICKER_MAPPING_VALUE))
                .map(x -> Objects.equals(x.getLeft(), JOIN_ENDLESS_FUTURE_MAPPING_VALUE)
                        ? Pair.of(x.getRight().substring(0,2) + "!1", x.getRight()) // маппим в бесконечный фьючерс со склеиванием интервалов
                        : x)
                .forEach(spotTickerCode2tickerCode -> {
                    var spotTickerCode = spotTickerCode2tickerCode.getLeft(); // потовый инструмент, по которому будет технический анализ
                    var futureTickerCode = spotTickerCode2tickerCode.getRight(); // фьючерс у которого нужно проставить ссылку на спот
                    // если спота нет, как например для бесконечных фьючей, то создаем свой.
                    var optTickerByTickerCode = tickersRepository.findTickerByTickerCode(spotTickerCode);
                    if (optTickerByTickerCode.isEmpty()) {
                        tickersRepository.saveInstrument(Tickers.builder()
                                .tickerCode(spotTickerCode)
                                .ticker(spotTickerCode)
                                .figi(spotTickerCode)
                                .description("endless future for " + futureTickerCode)
                                .exchange("RTS")
                                .provider(TRV_PROVIDER_TINKOFF)
                                .loadPriority(100)
                                .build());
                    }
                    // теперь можем обновить фьючерс, указав ссылку на спот.
                    tickersRepository.updateTickerSpotTickerCode(futureTickerCode, spotTickerCode);
                });
    }

    private Pair<String, String> getTickerSearchPart2FutureTickerCode(Tickers ticker) {
        String description = ticker.getDescription();
        Pattern pattern = Pattern.compile("-\\d{1,2}\\.\\d{2}");
        Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            int start = matcher.start();         // начало совпадения (-3.26)
            int end = matcher.end();             // конец совпадения
            String leftPart = description.substring(0, start);   // до "-3.26"
            String rightPart = description.substring(end).trim(); // после "-3.26", обрезаем пробелы
            return Pair.of(leftPart, ticker.getTickerCode());
        } else {
            return Pair.of(description, ticker.getTickerCode());
        }
    }

    private void saveFutureWithSpotLink(Future future, InstrumentShort spotTicker) {
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(spotTicker.getUid())
                        .ticker(spotTicker.getTicker())
                        .figi(spotTicker.getFigi())
                        .description(spotTicker.getName())
                        .marketType(null)
                        .exchange("RTS")
                        .precision(null)
                        .lot(spotTicker.getLot())
                        .go(null)
                        .expiration(null)
                        .currency("RUB")
                        .provider(TRV_PROVIDER_TINKOFF)
                        .status(null)
                        .loadPriority(100)
                        .spotTickerCode(null)
                        .build()
        );
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(future.getUid())
                        .ticker(future.getTicker())
                        .figi(future.getFigi())
                        .description(future.getName())
                        .marketType(TRV_FUTURES_ASSET_TYPE)
                        .exchange(future.getExchange())
                        .precision(null)
                        .lot(future.getLot())
                        .go(future.getDshortMin().getNano())
                        .expiration(
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(future.getExpirationDate().getSeconds()),
                                        ZoneId.systemDefault())
                        )
                        .currency(future.getCurrency())
                        .provider(TRV_PROVIDER_TINKOFF) // планируем торговать через финам поэтому подменяем провайдера.
                        .status(null)
                        .loadPriority(0)
                        .spotTickerCode(spotTicker.getUid())
                        .build()
        );
    }

    private void saveFutureWithoutLink(Future future) {
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(future.getUid())
                        .ticker(future.getTicker())
                        .figi(future.getFigi())
                        .description(future.getName())
                        .marketType(TRV_FUTURES_ASSET_TYPE)
                        .exchange(future.getExchange())
                        .precision(null)
                        .lot(future.getLot())
                        .go(future.getDshortMin().getNano())
                        .expiration(
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(future.getExpirationDate().getSeconds()),
                                        ZoneId.systemDefault())
                        )
                        .currency(future.getCurrency())
                        .provider(TRV_PROVIDER_TINKOFF)
                        .status(null)
                        .loadPriority(0)
                        .spotTickerCode(null)
                        .build()
        );
    }



}
