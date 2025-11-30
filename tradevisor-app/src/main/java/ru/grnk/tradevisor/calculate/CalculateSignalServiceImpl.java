package ru.grnk.tradevisor.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.telegram.webhook.TelegramApiClient;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static ru.grnk.tradevisor.integration.telegram.TelegramMessageBuilder.sendSimpleMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateSignalServiceImpl {

    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;
    private final SignalsRepository signalsRepository;
    private final List<IStrategy> strategies;
    private final TelegramApiClient telegramApiClient;
    private final TradevisorProperties tradevisorProperties;

    @Value("${app.calculate.batch-size:1000}")
    private int batchSize;

    private final AtomicLong lastTelegramUpdate = new AtomicLong(0);
    private static final long MIN_UPDATE_INTERVAL = 5000;

    @Scheduled(fixedRateString = "${app.calculate.delay}")
    public void doWork() {
        log.info("calculate all signals mf");
        int totalTickersCount = tickersRepository.getUnpublishedTickersCount();
        if (totalTickersCount == 0) {
            log.info("нет тикеров ждем когда появятся");
            sendTelegramLogMessage("Нет тикеров для обработки. Ждем появления новых.");
            return;
        }
        Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
        SendMessage startMessage = sendSimpleMessage(chatId, tradevisorProperties.integration().telegram().supergroup().logsThreadId(),
                "🚀 Начало расчета сигналов...\n" +
                        "📊 Всего тикеров: " + totalTickersCount + "\n" +
                        "🕐 Время начала: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        String messageId = null;
        try {
            Message msg = telegramApiClient.sendAndGetMessage(startMessage);
            messageId = String.valueOf(msg.getMessageId());
            lastTelegramUpdate.set(System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("Не удалось отправить начальное сообщение в Telegram", e);
        }
        try {
            int processedCount = processTickersWithUpdates(totalTickersCount, chatId, messageId);
            sendTelegramLogMessage("✅ Расчет сигналов завершен!\n" +
                    "📊 Обработано тикеров: " + processedCount + "/" + totalTickersCount + "\n" +
                    "🕐 Время окончания: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            String errorMessage = "❌ Ошибка при расчете сигналов: " + e.getMessage();
            log.error(errorMessage, e);
            sendTelegramErrorMessage(errorMessage);
            throw new RuntimeException("Ошибка при расчете сигналов", e);
        }
        log.info("all signals calculated");
    }

    private int processTickersWithUpdates(int totalTickersCount, Long chatId, String messageId) {
        int offset = 0;
        List<Tickers> tickersBatch;
        int processedCount = 0;
        DecimalFormat df = new DecimalFormat("#.##");
        do {
            tickersBatch = tickersRepository.getUnpublishedTickersBatch(batchSize, offset);
            if (tickersBatch.isEmpty()) {
                break;
            }
            for (Tickers t : tickersBatch) {
                try {
                    var lastTickTime = marketDataRepository.getLatestTickTime(t.getTickerCode(), 30);
                    strategies.forEach(s -> {
                        var candles = marketDataRepository.fetchMarketDataForLast(s.barsRequiredToCalcStrategy(), t.getTickerCode());
                        if (candles.size() < s.barsRequiredToCalcStrategy()) return;
                        TrvCalculationResult result = s.calculate(candles);
                        if (result.direction() != TradingDirection.UNKNOWN) {
                            String signalDescription = String.join(". ",
                                    t.getTicker(), t.getExchange(), t.getProvider(),
                                    Optional.ofNullable(result.description()).orElse("")
                            );
                            signalsRepository.saveSignal(result, t.getTickerCode(), s.getStrategyUniqueName(), lastTickTime, signalDescription);
                        }
                    });
                    processedCount++;
                    if (processedCount % 500 == 0 || processedCount == totalTickersCount) {
                        long currentTime = System.currentTimeMillis();
                        long lastUpdate = lastTelegramUpdate.get();
                        if (currentTime - lastUpdate >= MIN_UPDATE_INTERVAL) {
                            if (lastTelegramUpdate.compareAndSet(lastUpdate, currentTime)) {
                                updateProgressMessage(chatId, messageId, processedCount, totalTickersCount, t.getTickerCode(), df);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке тикера {}: {}", t.getTickerCode(), e.getMessage(), e);
                }
            }
            offset += batchSize;
        } while (tickersBatch.size() == batchSize);
        return processedCount;
    }

    private void updateProgressMessage(Long chatId, String messageId, int processedCount, int totalCount,
                                       String currentTicker, DecimalFormat df) {
        try {
            double percentage = (double) processedCount / totalCount * 100;
            String progressText = String.format(
                    "📊 Расчет сигналов в процессе...\n" +
                            "📈 Прогресс: %d/%d (%s%%)\n" +
                            "💼 Текущий тикер: %s\n" +
                            "⏱️ Время: %s",
                    processedCount, totalCount, df.format(percentage),
                    currentTicker, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            );
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId.toString());
                editMessage.setMessageId(Integer.parseInt(messageId));
                editMessage.setText(progressText);
                telegramApiClient.editMessageText(editMessage);
            } else {
                sendTelegramLogMessage(progressText);
            }
        } catch (Exception e) {
            log.warn("Не удалось обновить сообщение прогресса в Telegram", e);
        }
    }

    private void sendTelegramMessage(String text, Integer threadId) {
        try {
            Long chatId = tradevisorProperties.integration().telegram().supergroup().chatId();
            SendMessage message = sendSimpleMessage(chatId, threadId, text);
            telegramApiClient.sendAndGetMessage(message);
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение в Telegram: {}", text, e);
        }
    }

    private void sendTelegramErrorMessage(String text) {
        sendTelegramMessage(text, tradevisorProperties.integration().telegram().supergroup().errorsThreadId());
    }

    private void sendTelegramLogMessage(String text) {
        sendTelegramMessage(text, tradevisorProperties.integration().telegram().supergroup().logsThreadId());
    }

}
