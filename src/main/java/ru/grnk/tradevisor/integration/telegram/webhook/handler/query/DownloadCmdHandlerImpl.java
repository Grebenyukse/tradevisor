package ru.grnk.tradevisor.integration.telegram.webhook.handler.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;
import ru.grnk.tradevisor.notify.plot.PlotService;

import java.util.List;
import java.util.Objects;

import static ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder.getSignalIdFromQuery;

@Service
@RequiredArgsConstructor
public class DownloadCmdHandlerImpl implements TgCallbackQueryHandler {

    private final TelegramApiClient telegramApiClient;
    private final SignalsRepository signalsRepository;
    private final MarketDataRepository marketDataRepository;
    private final List<IStrategy> strategies;
    private final PlotService plotService;

    @Override
    public String commandStartsWith() {
        return "download/";
    }

    @Override
    public void handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Integer signalId = getSignalIdFromQuery(callbackData);
        Signals signal = signalsRepository.findSignalBySignalId(signalId).orElseThrow();
        var strat = strategies.stream().filter(s -> Objects.equals(s.getStrategyUniqueName(), signal.getName()))
                .findFirst()
                .orElseThrow();
        var downloadChartUrl = plotService.saveCandlestickChartToFile(signal, true);
        List<MarketData> marketData = marketDataRepository.fetchMarketDataForLast(strat.barsRequiredToCalcStrategy(), signal.getTickerCode());
        String fileContent = formatAsTable(marketData);
        String fileName = signal.getName() + "__" + signal.getId() + "__" + signal.getTickerCode() + " __" + signal.getCreatedAt();
        String caption = "chartDownloadUrl: " + downloadChartUrl;
        telegramApiClient.sendDocument(callbackQuery.getMessage(), fileContent, fileName, caption);
    }

    public static String formatAsTable(List<MarketData> marketDataList) {
        if (marketDataList == null || marketDataList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s | %-8s | %-8s | %-8s | %-8s | %-25s%n",
                "TICKER", "OPEN", "HIGH", "LOW", "CLOSE", "TIME"));
        sb.append("----------------------------------------------" +
                "------------------------------------------------------\n");
        for (MarketData data : marketDataList) {
            sb.append(String.format("%-10s | %-8.2f | %-8.2f | %-8.2f | %-8.2f | %-25s%n",
                    data.getTickerCode(),
                    data.getOpen(),
                    data.getHigh(),
                    data.getLow(),
                    data.getClose(),
                    data.getTime().toString()));
        }

        return sb.toString();
    }
}
