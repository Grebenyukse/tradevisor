package ru.grnk.tradevisor.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.integration.telegram.TelegramMessageService;
import ru.grnk.tradevisor.notify.plot.PlotService;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final SignalsRepository signalsRepository;
    private final PlotService plotService;
    private final TelegramMessageService telegramMessageService;

    @Transactional
    public void publishMessage(Signals signal) {
        String image = plotService.saveCandlestickChartToFile(signal, true);
        if (image == null ) return;
        telegramMessageService.sendMessage(image, getTitle(signal), getText(signal), signal.getId());
        signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.PUBLISHED);
    }

    private static String getTitle(Signals signal) {
        return signal.getTickerCode() + " " + TradingDirection.from(signal.getDirection()).name() + " " + signal.getName();
    }

    private static String getText(Signals signal) {
        return TradingDirection.from(signal.getDirection()).name()
                + ". \n PriceOpen: " + signal.getPriceOpen()
                + ". \n TakeProfit: " + signal.getTakeProfit()
                + ". \n StopLoss: " + signal.getStopLoss()
                + ". \n ProducedAt: " + signal.getCreatedAt();
    }
}