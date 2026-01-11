package ru.grnk.tradevisor.collect.prices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PricesLoaderServiceImpl {

    public static final Integer TICKERS_BATCH_LOAD = 1000;
    private final TickersRepository tickersRepository;
    private final List<PricesLoader> loaders;
    private final PriceLoadingErrorHandler errorHandler;
    private final TelegramNotificationService telegramService;
    private final BindTradeFuturesService bindTradeFuturesService;
    private final Executor priceLoadingExecutor;

    @Autowired
    public PricesLoaderServiceImpl(TickersRepository tickersRepository,
                                   List<PricesLoader> loaders,
                                   PriceLoadingErrorHandler errorHandler,
                                   TelegramNotificationService telegramService,
                                   BindTradeFuturesService bindTradeFuturesService,
                                   @Qualifier("priceLoadingExecutor")
                                   Executor priceLoadingExecutor) {
        this.tickersRepository = tickersRepository;
        this.loaders = loaders;
        this.errorHandler = errorHandler;
        this.telegramService = telegramService;
        this.bindTradeFuturesService = bindTradeFuturesService;
        this.priceLoadingExecutor = priceLoadingExecutor;
    }

    @Scheduled(fixedRateString = "${app.collect.prices.init-tickers.delay}")
    public void initTickers() {
        loaders
                .stream()
                .sorted(Comparator.comparingInt(PricesLoader::loadOrder))
                .forEach(PricesLoader::initTickers);
        bindTradeFuturesService.initTickers();
    }

    @Scheduled(fixedRateString = "${app.collect.prices.delay}")
    public void doWork() {
        if (tickersRepository.getAllTickersCount() == 0) {
            initTickers();
        }
        log.info("start collecting prices");
        LocalDateTime startTime = LocalDateTime.now();
        String messageId = telegramService.sendInitialMessage("🔄 Загрузка тикеров...");
        Integer totalTickersCount = tickersRepository.getAllTickersCount();
        if (totalTickersCount == 0) {
            log.info("Нет тикеров для загрузки");
            telegramService.updateMessage(messageId, "📭 Нет тикеров для загрузки котировок");
            return;
        }
        Map<String, Integer> provider2TickersCount = tickersRepository.getTickersCountByProvider();
        Map<String, Integer> providerProcessedCount = new ConcurrentHashMap<>();
        provider2TickersCount.keySet().forEach(provider -> providerProcessedCount.put(provider, 0));
        telegramService.sendStartMessage(messageId, totalTickersCount, provider2TickersCount, startTime);
        try {
            processAllProviders(provider2TickersCount, providerProcessedCount, messageId, startTime);
            telegramService.sendFinalMessage(messageId,
                    providerProcessedCount.values().stream().mapToInt(Integer::intValue).sum(),
                    totalTickersCount,
                    provider2TickersCount,
                    providerProcessedCount,
                    startTime);
        } catch (Exception e) {
            log.error("Ошибка загрузки котировок", e);
            telegramService.sendErrorMessage(messageId, e);
            throw new RuntimeException("Ошибка загрузки котировок", e);
        }
        log.info("historic candles loaded");
    }

    private void processAllProviders(Map<String, Integer> provider2TickersCount,
                                     Map<String, Integer> providerProcessedCount,
                                     String messageId,
                                     LocalDateTime startTime) throws InterruptedException {
        Map<String, PricesLoader> providerToLoader = loaders.stream()
                .collect(Collectors.toMap(PricesLoader::getProvider, loader -> loader));

        List<CompletableFuture<Void>> futures = provider2TickersCount.keySet().stream()
                .map(provider -> CompletableFuture.runAsync(() ->
                        processProvider(
                                provider,
                                providerToLoader.get(provider),
                                provider2TickersCount.get(provider),
                                providerProcessedCount,
                                messageId,
                                startTime
                        ), priceLoadingExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processProvider(String provider, PricesLoader loader,
                                 Integer totalTickersForProvider,
                                 Map<String, Integer> providerProcessedCount,
                                 String messageId, LocalDateTime startTime) {
        if (loader == null) {
            log.warn("No loader found for provider: {}", provider);
            return;
        }
        log.info("process load prices provider: {}", loader.getProvider());
        int processedForThisProvider = 0;
        long lastUpdate = System.currentTimeMillis();
        do {
            List<Tickers> tickers = tickersRepository.getAllTickers(provider, TICKERS_BATCH_LOAD, processedForThisProvider);
            for (Tickers ticker : tickers) {
                try {
                    loader.loadPrices(ticker);
                    processedForThisProvider++;
                    providerProcessedCount.merge(provider, 1, Integer::sum);

                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 30000) {
                        telegramService.sendProgressMessage(messageId,
                                providerProcessedCount.values().stream().mapToInt(Integer::intValue).sum(),
                                tickersRepository.getAllTickersCount(),
                                provider,
                                ticker.getTicker() + "@" + ticker.getExchange(),
                                (int) providerProcessedCount.keySet().stream().filter(p ->
                                        providerProcessedCount.get(p) >= tickersRepository.getTickersCountByProvider().get(p)).count(),
                                providerProcessedCount.size(),
                                tickersRepository.getTickersCountByProvider(),
                                providerProcessedCount,
                                startTime);
                        lastUpdate = now;
                    }
                } catch (Exception e) {
                    PriceLoadingErrorHandler.ErrorHandlerResult result = errorHandler.handleError(e, ticker, provider);
                    switch (result.getAction()) {
                        case SKIP:
                            processedForThisProvider++;
                            providerProcessedCount.merge(provider, 1, Integer::sum);
                            continue;
                        case RETRY:
                            sleep();
                            break;
                        case FAIL:
                            log.error("ticker: {}. provider: {}. неизвестная ошибка. ", ticker, provider, e);
                            sleep();
                            break;
                    }
                }
            }
        } while (processedForThisProvider < totalTickersForProvider);
        log.info("Provider {} completed with {} tickers processed", provider, processedForThisProvider);
    }

    private void sleep() {
        try {
            Thread.sleep(60000); // Ждем минуту перед повторной попыткой
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }
}
