package ru.grnk.tradevisor.notify.publish;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.integration.telegram.out.BotMsgSender;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final BotMsgSender botMsgSender;
    private final SignalsRepository signalsRepository;

    @Transactional
    public void publishMessage(Signals signal) {
        var botMessage = MapSignal2Message.from(signal);
        botMsgSender.sendMessage(botMessage);
        signalsRepository.updateSignalStatus(signal, TrvSignalStatus.PUBLISHED);
    }
}
