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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.grnk.tradevisor.collect.prices.Futures2SpotMap.*;
import static ru.grnk.tradevisor.integration.finam.FinamPricesService.TRV_PROVIDER_FINAM;

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
    public void initTickers() {
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
                        ? Pair.of(x.getRight().getTicker().substring(0,2) + "!1", x.getRight()) // маппим в бесконечный фьючерс со склеиванием интервалов
                        : x)
                .forEach(spotTickerCode2tickerCode -> {
                    var spotTickerCode = spotTickerCode2tickerCode.getLeft(); // потовый инструмент, по которому будет технический анализ
                    var futureTicker = spotTickerCode2tickerCode.getRight().getTicker(); // фьючерс у которого нужно проставить ссылку на спот
                    var futureTickerCode = spotTickerCode2tickerCode.getRight().getTickerCode(); // uid оригинального фьюча у которого будет размещена ссылка на спот
                    // если спота нет, как например для бесконечных фьючей, то создаем свой.
                    var optTickerByTickerCode = tickersRepository.findTickerByTickerCode(spotTickerCode);
                    if (optTickerByTickerCode.isEmpty()) {
                        tickersRepository.saveInstrument(Tickers.builder()
                                .tickerCode(spotTickerCode)
                                .ticker(spotTickerCode)
                                .description("null")
                                .exchange("RTS")
                                .provider(TRV_PROVIDER_TINKOFF)
                                .build());
                    }
                    // теперь можем обновить фьючерс, указав ссылку на спот.
                    tickersRepository.updateTickerSpotTickerCode(futureTickerCode, spotTickerCode);
                });
    }

    private Pair<String, Tickers> getTickerSearchPart2FutureTickerCode(Tickers ticker) {
        String description = ticker.getDescription();
        Pattern pattern = Pattern.compile("-\\d{1,2}\\.\\d{2}");
        Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            int start = matcher.start();         // начало совпадения (-3.26)
            String leftPart = description.substring(0, start);   // до "-3.26"
            return Pair.of(leftPart, ticker);
        } else {
            return Pair.of(description, ticker);
        }
    }

    private void saveFutureWithSpotLink(Future future, InstrumentShort spotTicker) {
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(spotTicker.getUid())
                        .ticker(spotTicker.getTicker())
                        .description(spotTicker.getName())
                        .exchange("RTS")
                        .currency("RUB")
                        .provider(TRV_PROVIDER_TINKOFF)
                        .build()
        );
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(future.getTicker() + "@" + "RTSX")
                        .ticker(future.getTicker())
                        .description(future.getName())
                        .exchange("RTSX")
                        .currency(future.getCurrency())
                        .provider(TRV_PROVIDER_FINAM) // планируем торговать через финам поэтому подменяем провайдера.
                        .spotTickerCode(spotTicker.getUid())
                        .build()
        );
    }

    private void saveFutureWithoutLink(Future future) {
        tickersRepository.saveInstrument(
                Tickers.builder()
                        .tickerCode(future.getTicker() + "@" + "RTSX")
                        .ticker(future.getTicker())
                        .description(future.getName())
                        .exchange(future.getExchange())
                        .currency(future.getCurrency())
                        .provider(TRV_PROVIDER_FINAM)
                        .build()
        );
    }



}
