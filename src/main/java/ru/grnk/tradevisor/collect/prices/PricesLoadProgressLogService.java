package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PricesLoadProgressLogService {

    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    public void logStartMessage(String messageId, int totalTickersCount,
                                Map<String, Integer> provider2TickersCount, LocalDateTime startTime) {
        StringBuilder initialMessage = new StringBuilder();
        initialMessage
                .append("[msgId:]").append(messageId)
                .append("🚀 Начало загрузки котировок...\n")
                .append("📊 Всего тикеров: ").append(totalTickersCount).append("\n")
                .append("🏢 Провайдеры:\n");
        for (Map.Entry<String, Integer> entry : provider2TickersCount.entrySet()) {
            initialMessage.append("  • ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append(" тикеров\n");
        }
        initialMessage.append("🕐 Время начала: ")
                .append(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        log.info(initialMessage.toString());
    }

    public void logProgressMessage(String messageId, int processedCount, int totalCount,
                                   String currentProvider, String currentTicker,
                                   int completedProviders, int totalProviders,
                                   Map<String, Integer> providerTotalCount,
                                   Map<String, Integer> providerProcessedCount,
                                   LocalDateTime startTime) {
        double percentage = (double) processedCount / totalCount * 100;
        StringBuilder progressText = new StringBuilder();
        progressText
                .append("[msgId:]").append(messageId)
                .append("📊 Загрузка котировок в процессе...\n")
                .append("📈 Прогресс: ").append(processedCount).append("/").append(totalCount)
                .append(" (").append(decimalFormat.format(percentage)).append("%)\n")
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
                    .append(" (").append(decimalFormat.format(providerPercentage)).append("%)\n");
        }
        progressText.append("⏱️ Время: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        log.info(progressText.toString());
    }

    public void logFinalMessage(String messageId, int processedCount, int totalCount,
                                Map<String, Integer> providerTotalCount,
                                Map<String, Integer> providerProcessedCount,
                                LocalDateTime startTime) {
        StringBuilder finalMessage = new StringBuilder();

        // Вычисляем время окончания и продолжительность
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        finalMessage
                .append("[msgId:]").append(messageId)
                .append("✅ Загрузка котировок завершена!\n")
                .append("📊 Обработано тикеров: ").append(processedCount).append("/").append(totalCount)
                .append(" (").append(decimalFormat.format((double) processedCount / totalCount * 100)).append("%)\n")
                .append("🏢 По провайдерам:\n");

        for (Map.Entry<String, Integer> entry : providerTotalCount.entrySet()) {
            String provider = entry.getKey();
            int total = entry.getValue();
            int processed = providerProcessedCount.getOrDefault(provider, 0);
            double percentage = total > 0 ? (double) processed / total * 100 : 0;

            finalMessage.append("  • ").append(provider).append(": ")
                    .append(processed).append("/").append(total)
                    .append(" (").append(decimalFormat.format(percentage)).append("%)\n");
        }

        finalMessage.append("🕐 Время начала: ")
                .append(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n")
                .append("🏁 Время окончания: ")
                .append(endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n")
                .append("⏱️ Затраченное время: ");

        if (hours > 0) {
            finalMessage.append(hours).append(" ч ");
        }
        if (minutes > 0) {
            finalMessage.append(minutes).append(" мин ");
        }
        finalMessage.append(seconds).append(" сек");

        log.info(finalMessage.toString());
    }
}
