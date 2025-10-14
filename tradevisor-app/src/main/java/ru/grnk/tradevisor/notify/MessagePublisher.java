package ru.grnk.tradevisor.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.integration.telegram.out.BotMsgSender;
import ru.grnk.tradevisor.notify.plot.PlotService;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final BotMsgSender botMsgSender;
    private final SignalsRepository signalsRepository;
    private final PlotService plotService;

    @Transactional
    public void publishMessage(Signals signal) {
        var botMessage = MapSignal2Message.from(signal);
        plotService.saveCandlestickChartToFile("bf341bb3-db3d-43fe-b844-b934a24828ef", 1200, 800);
        botMsgSender.sendMessage(botMessage);
        signalsRepository.updateSignalStatus(signal, TrvSignalStatus.PUBLISHED);
    }
}
