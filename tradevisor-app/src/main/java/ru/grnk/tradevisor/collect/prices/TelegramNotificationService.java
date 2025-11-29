package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendSimpleMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public String sendInitialMessage(String text) {
        try {
            Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
            int logThreadId = tradevisorProperties.integration().telegram().supergroup().logsThreadId();
            SendMessage message = sendSimpleMessage(chatId, logThreadId, text);
            Message msg = telegramApiClient.sendAndGetMessage(message);
            return msg != null ? String.valueOf(msg.getMessageId()) : null;
        } catch (Exception e) {
            log.warn("Не удалось отправить начальное сообщение в Telegram: {}", text, e);
            return null;
        }
    }

    public void sendStartMessage(String messageId, int totalTickersCount,
                                 Map<String, Integer> provider2TickersCount, LocalDateTime startTime) {
        try {
            StringBuilder initialMessage = new StringBuilder();
            initialMessage.append("🚀 Начало загрузки котировок...\n")
                    .append("📊 Всего тикеров: ").append(totalTickersCount).append("\n")
                    .append("🏢 Провайдеры:\n");
            for (Map.Entry<String, Integer> entry : provider2TickersCount.entrySet()) {
                initialMessage.append("  • ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append(" тикеров\n");
            }
            initialMessage.append("🕐 Время начала: ")
                    .append(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            updateMessage(messageId, initialMessage.toString());
        } catch (Exception e) {
            log.warn("Не удалось отправить начальное сообщение в Telegram", e);
        }
    }

    public void sendProgressMessage(String messageId, int processedCount, int totalCount,
                                    String currentProvider, String currentTicker,
                                    int completedProviders, int totalProviders,
                                    Map<String, Integer> providerTotalCount,
                                    Map<String, Integer> providerProcessedCount,
                                    LocalDateTime startTime) {
        try {
            double percentage = (double) processedCount / totalCount * 100;
            StringBuilder progressText = new StringBuilder();
            progressText.append("📊 Загрузка котировок в процессе...\n")
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

            updateMessage(messageId, progressText.toString());
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение прогресса в Telegram", e);
        }
    }

    public void sendFinalMessage(String messageId, int processedCount, int totalCount,
                                 Map<String, Integer> providerTotalCount,
                                 Map<String, Integer> providerProcessedCount,
                                 LocalDateTime startTime) {
        try {
            StringBuilder finalMessage = new StringBuilder();

            // Вычисляем время окончания и продолжительность
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            finalMessage.append("✅ Загрузка котировок завершена!\n")
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

            updateMessage(messageId, finalMessage.toString());
        } catch (Exception e) {
            log.warn("Не удалось отправить финальное сообщение в Telegram", e);
        }
    }

    public void sendErrorMessage(String messageId, Exception e) {
        try {
            Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
            Integer threadId = tradevisorProperties.integration().telegram().supergroup().errorsThreadId();
            String errorMessage = String.format("❌ Ошибка загрузки котировок: https://t.me/c/%S/%s \n %s", chatId, messageId, e.getMessage()) ;
            sendSimpleMessage(chatId, threadId, errorMessage);
        } catch (Exception ex) {
            log.warn("Не удалось отправить сообщение об ошибке в Telegram", ex);
        }
    }

    public void updateMessage(String messageId, String text) {
        try {
            if (messageId != null) {
                Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText(text);
                telegramApiClient.editMessageText(editMessage);
            } else {
                int errorsThreadId = tradevisorProperties.integration().telegram().supergroup().errorsThreadId();
                sendMessage(text, errorsThreadId);
            }
        } catch (Exception e) {
            log.warn("Не удалось обновить сообщение в Telegram: {}", text, e);
        }
    }

    public void sendMessage(String text, int threadId) {
        try {
            Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
            SendMessage message = sendSimpleMessage(chatId, threadId, text);
            telegramApiClient.sendAndGetMessage(message);
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение в Telegram: {}", text, e);
        }
    }
}
