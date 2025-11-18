package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        // Отправляем начальное сообщение и сохраняем его ID
        String messageId = sendInitialTelegramMessage("🔄 Инициализация тикеров...");
        loaders.forEach(PricesLoader::initTickers);

        Integer totalTickersCount = tickersRepository.getAllTickersCount();
        if (totalTickersCount == 0) {
            log.info("Нет тикеров для загрузки");
            updateTelegramMessage(messageId, "📭 Нет тикеров для загрузки котировок");
            return;
        }

        Map<String, Integer> provider2TickersCount = tickersRepository.getTickersCountByProvider();
        Map<String, Integer> providerProcessedCount = new ConcurrentHashMap<>();

        // Инициализируем счетчики обработанных тикеров для каждого провайдера
        provider2TickersCount.keySet().forEach(provider -> providerProcessedCount.put(provider, 0));

        StringBuilder initialMessage = new StringBuilder();
        initialMessage.append("🚀 Начало загрузки котировок...\n")
                .append("📊 Всего тикеров: ").append(totalTickersCount).append("\n")
                .append("🏢 Провайдеры:\n");

        for (Map.Entry<String, Integer> entry : provider2TickersCount.entrySet()) {
            initialMessage.append("  • ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append(" тикеров\n");
        }

        initialMessage.append("🕐 Время начала: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        updateTelegramMessage(messageId, initialMessage.toString());

        Map<String, Integer> providerOffsets = new ConcurrentHashMap<>();
        Map<String, Boolean> providerCompleted = new ConcurrentHashMap<>();
        DecimalFormat df = new DecimalFormat("#.##");

        try {
            Map<String, PricesLoader> providerToLoader = new HashMap<>();
            for (PricesLoader loader : loaders) {
                providerToLoader.put(loader.getProvider(), loader);
            }
            List<String> providers = new ArrayList<>(provider2TickersCount.keySet());
            int currentProviderIndex = 0;
            int processedCount = 0;
            long lastUpdate = System.currentTimeMillis();

            while (providerCompleted.size() < providers.size()) {
                String currentProvider = providers.get(currentProviderIndex);
                if (Boolean.TRUE.equals(providerCompleted.get(currentProvider))) {
                    currentProviderIndex = (currentProviderIndex + 1) % providers.size();
                    continue;
                }
                PricesLoader loader = providerToLoader.get(currentProvider);
                if (loader == null) {
                    log.warn("No loader found for provider: {}", currentProvider);
                    providerCompleted.put(currentProvider, true);
                    currentProviderIndex = (currentProviderIndex + 1) % providers.size();
                    continue;
                }
                int offset = providerOffsets.getOrDefault(currentProvider, 0);
                List<Tickers> tickers = tickersRepository.getAllTickers(currentProvider, TICKERS_BATCH_LOAD, offset);
                boolean resourceExhausted = false;

                for (Tickers ticker : tickers) {
                    try {
                        loader.loadPrices(ticker.getTickerCode());
                        processedCount++;
                        providerProcessedCount.merge(currentProvider, 1, Integer::sum);

                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 30000 || processedCount % 50 == 0) {
                            updateProgressMessage(messageId, processedCount, totalTickersCount,
                                    currentProvider, ticker.getTickerCode(),
                                    providerCompleted.size(), providers.size(),
                                    provider2TickersCount, providerProcessedCount, df);
                            lastUpdate = now;
                        }

                    } catch (Exception e) {
                        if (e.getMessage().contains("Security id doesn't exist for mic")) {
                            tickersRepository.markTickerFailedByQuotes(ticker.getTickerCode());
                            log.warn("ticker {} excluded as unknown. will not be requested next time.", ticker.getTickerCode());
                            processedCount++;
                            providerProcessedCount.merge(currentProvider, 1, Integer::sum);
                            continue;
                        }
                        if (e.getMessage().contains("Api token could not be verified")) {
                            log.error("unauthenticated when loading ticker: {}", ticker.getTickerCode());
                            processedCount++;
                            providerProcessedCount.merge(currentProvider, 1, Integer::sum);
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
                    providerCompleted.put(currentProvider, true);
                    log.info("Provider {} completed", currentProvider);
                    // Для завершения провайдера отправляем отдельное сообщение
                    sendTelegramMessage(String.format("✅ Провайдер %s завершен", currentProvider));
                } else {
                    providerOffsets.put(currentProvider, offset + tickers.size());
                }
                currentProviderIndex = (currentProviderIndex + 1) % providers.size();
            }

            updateTelegramMessage(messageId, buildFinalMessage(processedCount, totalTickersCount,
                    provider2TickersCount, providerProcessedCount));

        } catch (Exception e) {
            String errorMessage = "❌ Ошибка загрузки котировок: " + e.getMessage();
            log.error(errorMessage, e);
            updateTelegramMessage(messageId, errorMessage);
            throw new RuntimeException("oshibka", e);
        }

        log.info("historic candles loaded");
    }

    private String buildFinalMessage(int processedCount, int totalCount,
                                     Map<String, Integer> providerTotalCount,
                                     Map<String, Integer> providerProcessedCount) {
        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder finalMessage = new StringBuilder();
        finalMessage.append("✅ Загрузка котировок завершена!\n")
                .append("📊 Обработано тикеров: ").append(processedCount).append("/").append(totalCount)
                .append(" (").append(df.format((double) processedCount / totalCount * 100)).append("%)\n")
                .append("🏢 По провайдерам:\n");

        for (Map.Entry<String, Integer> entry : providerTotalCount.entrySet()) {
            String provider = entry.getKey();
            int total = entry.getValue();
            int processed = providerProcessedCount.getOrDefault(provider, 0);
            double percentage = total > 0 ? (double) processed / total * 100 : 0;

            finalMessage.append("  • ").append(provider).append(": ")
                    .append(processed).append("/").append(total)
                    .append(" (").append(df.format(percentage)).append("%)\n");
        }

        finalMessage.append("🕐 Время окончания: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        return finalMessage.toString();
    }

    private void updateProgressMessage(String messageId, int processedCount, int totalCount, String currentProvider,
                                       String currentTicker, int completedProviders, int totalProviders,
                                       Map<String, Integer> providerTotalCount,
                                       Map<String, Integer> providerProcessedCount,
                                       DecimalFormat df) {
        try {
            double percentage = (double) processedCount / totalCount * 100;
            StringBuilder progressText = new StringBuilder();
            progressText.append("📊 Загрузка котировок в процессе...\n")
                    .append("📈 Прогресс: ").append(processedCount).append("/").append(totalCount)
                    .append(" (").append(df.format(percentage)).append("%)\n")
                    .append("🏢 Провайдеры: ").append(completedProviders).append("/").append(totalProviders)
                    .append(" завершено\n")
                    .append("💼 Текущий: ").append(currentProvider).append(" - ").append(currentTicker).append("\n")
                    .append("📊 Статистика по провайдерам:\n");

            for (Map.Entry<String, Integer> entry : providerTotalCount.entrySet()) {
                String provider = entry.getKey();
                int total = entry.getValue();
                int processed = providerProcessedCount.getOrDefault(provider, 0);
                double providerPercentage = total > 0 ? (double) processed / total * 100 : 0;

                progressText.append("  • ").append(provider).append(": ")
                        .append(processed).append("/").append(total)
                        .append(" (").append(df.format(providerPercentage)).append("%)\n");
            }

            progressText.append("⏱️ Время: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            updateTelegramMessage(messageId, progressText.toString());
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение прогресса в Telegram", e);
        }
    }

    private String sendInitialTelegramMessage(String text) {
        try {
            Long chatId = Long.parseLong(tradevisorProperties.integration().telegram().chatId());
            SendMessage message = sendSimpleMessage(chatId, text);
            Message msg = telegramApiClient.sendAndGetMessage(message);
            return msg != null ? String.valueOf(msg.getMessageId()) : null;
        } catch (Exception e) {
            log.warn("Не удалось отправить начальное сообщение в Telegram: {}", text, e);
            return null;
        }
    }

    private void updateTelegramMessage(String messageId, String text) {
        try {
            if (messageId != null) {
                Long chatId = Long.parseLong(tradevisorProperties.integration().telegram().chatId());
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText(text);
                telegramApiClient.editMessageText(editMessage);
            } else {
                sendTelegramMessage(text);
            }
        } catch (Exception e) {
            log.warn("Не удалось обновить сообщение в Telegram: {}", text, e);
        }
    }

    private void sendTelegramMessage(String text) {
        try {
            Long chatId = Long.parseLong(tradevisorProperties.integration().telegram().chatId());
            SendMessage message = sendSimpleMessage(chatId, text);
            telegramApiClient.sendAndGetMessage(message);
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение в Telegram: {}", text, e);
        }
    }
}
