package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendSimpleMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class PricesLoaderServiceImpl {

    public static final String SHARES_TICKER_NAMES_LOADED = "shares_ticker_names_loaded";
    public static final Integer TICKERS_BATCH_LOAD = 1000;
    private final TickersRepository tickersRepository;
    private final ParametersRepository parametersRepository;
    private final List<PricesLoader> loaders;
    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    @SneakyThrows
    @Scheduled(cron = "${app.collect.prices.cron}")
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if (!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED))) {
            log.debug("init tickers");
            loaders.forEach(PricesLoader::initTickers);
            log.debug("tickers saved");
        }
        Integer totalTickersCount = tickersRepository.getAllTickersCount();
        Map<String, Integer> provider2TickersCount = tickersRepository.getTickersCountByProvider();
        Map<String, Integer> providerOffsets = new ConcurrentHashMap<>();
        Set<String> completedProviders = ConcurrentHashMap.newKeySet();
        try (var pb = new ProgressBarBuilder()
                .setTaskName("Download prices")
                .setInitialMax(totalTickersCount)
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build()) {
            Map<String, PricesLoader> providerToLoader = new HashMap<>();
            for (PricesLoader loader : loaders) {
                providerToLoader.put(loader.getProvider(), loader);
            }
            List<String> providers = new ArrayList<>(provider2TickersCount.keySet());
            int currentProviderIndex = 0;
            while (completedProviders.size() < providers.size()) {
                String currentProvider = providers.get(currentProviderIndex);
                if (completedProviders.contains(currentProvider)) {
                    currentProviderIndex = (currentProviderIndex + 1) % providers.size();
                    continue;
                }
                PricesLoader loader = providerToLoader.get(currentProvider);
                if (loader == null) {
                    log.warn("No loader found for provider: {}", currentProvider);
                    completedProviders.add(currentProvider);
                    currentProviderIndex = (currentProviderIndex + 1) % providers.size();
                    continue;
                }
                int offset = providerOffsets.getOrDefault(currentProvider, 0);
                List<Tickers> tickers = tickersRepository.getAllTickers(currentProvider, TICKERS_BATCH_LOAD, offset);
                boolean resourceExhausted = false;
                for (Tickers ticker : tickers) {
                    try {
                        loader.loadPrices(ticker.getTickerCode());
                        pb.step();
                        pb.setExtraMessage(currentProvider + ": " + ticker.getTickerCode());
                    } catch (Exception e) {
                        if (e.getMessage().contains("Security id doesn't exist for mic")) {
                            tickersRepository.markTickerFailedByQuotes(ticker.getTickerCode());
                            log.warn("ticker {} excluded as unknown. will not be requested next time.", ticker.getTickerCode());
                            pb.step();
                            pb.setExtraMessage(currentProvider + ": ticker excluded: " + ticker.getTickerCode());
                            continue;
                        }
                        if (e.getMessage().contains("Api token could not be verified")) {
                            log.error("unauthenticated when loading ticker: {}", ticker.getTickerCode());
                            pb.step();
                            pb.setExtraMessage(currentProvider + ": token could not be verified. ticker: " + ticker.getTickerCode());
                            continue;
                        }
                        if (e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                            log.warn("RESOURCE EXHAUSTED for provider: {}", currentProvider);
                            resourceExhausted = true;
                            providerOffsets.put(currentProvider, offset);
                            break;
                        }
                        throw new RuntimeException(e);
                    }
                }
                if (resourceExhausted) {
                    currentProviderIndex = (currentProviderIndex + 1) % providers.size();
                    continue;
                }
                if (tickers.isEmpty() || tickers.size() < TICKERS_BATCH_LOAD) {
                    completedProviders.add(currentProvider);
                    log.info("Provider {} completed", currentProvider);
                } else {
                    providerOffsets.put(currentProvider, offset + tickers.size());
                }
                currentProviderIndex = (currentProviderIndex + 1) % providers.size();
            }
        } catch (Exception e) {
            telegramApiClient.sendMessage(sendSimpleMessage(Long.parseLong(
                            tradevisorProperties.integration().telegram().chatId()),
                    "Ошибка скачивания котировок: " + e.getCause().toString()));

            throw new RuntimeException("oshibka", e);
        }
        log.info("historic candles loaded");
    }
}
