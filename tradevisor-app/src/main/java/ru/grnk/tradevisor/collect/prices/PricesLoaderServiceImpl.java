package ru.grnk.tradevisor.collect.prices;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toMap;
import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendSimpleMessage;

@Service
@Slf4j
public class PricesLoaderServiceImpl {

    public static final String SHARES_TICKER_NAMES_LOADED = "shares_ticker_names_loaded";
    private final TickersRepository tickersRepository;
    private final ParametersRepository parametersRepository;
    private final Map<String, PricesLoader> loaders;
    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    @Autowired
    public PricesLoaderServiceImpl(TickersRepository tickersRepository,
                                   ParametersRepository parametersRepository,
                                   TelegramApiClient telegramApiClient,
                                   TradevisorProperties tradevisorProperties,
                                   List<PricesLoader> pricesLoaders) {
        this.tickersRepository = tickersRepository;
        this.parametersRepository = parametersRepository;
        this.telegramApiClient = telegramApiClient;
        this.tradevisorProperties = tradevisorProperties;
        this.loaders = pricesLoaders.stream().collect(toMap(PricesLoader::getProvider, x -> x));
    }

    @SneakyThrows
    @Scheduled(cron = "${app.collect.prices.cron}")
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if (!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED))) {
            log.debug("init tickers");
            loaders.values().forEach(PricesLoader::initTickers);
            log.debug("tickers saved");
        }
        List<Tickers> tickers = tickersRepository.getAllTickers();
        try(var pb = new ProgressBarBuilder()
                .setTaskName("Download prices")
                .setInitialMax(tickers.size())
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build()) {
            tickers.stream().filter(x -> x.getProvider() != null)
                    .forEach(x -> {
                        try {
                            var loader = loaders.get(x.getProvider());
                            if (loader == null) return;
                            loader.loadPrices(x.getTickerCode());
                            pb.step();
                            pb.setExtraMessage(x.getTickerCode());
                        } catch (Exception e) {
                            if (e.getMessage().contains("Security id doesn't exist for mic")) {
                                tickersRepository.markTickerFailedByQuotes(x.getTickerCode());
                                log.warn("ticker {} excluded as unknown. will not be requested next time.", x.getTickerCode());
                                pb.step();
                                pb.setExtraMessage("ticker excluded: " + x.getTickerCode());
                                return;
                            }
                            if (e.getMessage().contains("Api token could not be verified")) {
                                log.error("unauthenticated when loading ticker: {}", x.getTickerCode());
                                pb.step();
                                pb.setExtraMessage("token could not be verified. ticker: " + x.getTickerCode());
                                return;
                            }
                            if (e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                                log.warn("RESOURCE EXHAUSTED");
                                try {
                                    sleep(1000L);
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                                pb.step();
                                pb.setExtraMessage("RESOURCE EXHAUSTED. skip ticker: " + x.getTickerCode());
                            }
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            telegramApiClient.sendMessage(sendSimpleMessage(Long.parseLong(
                    tradevisorProperties.integration().telegram().chatId()),
                    "Ошибка скачивания котировок: " + e.getCause().toString()));

            throw new RuntimeException("oshibka", e);
        }
        log.info("historic candles loaded");
    }
}