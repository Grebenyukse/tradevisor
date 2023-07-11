package ru.grnk.tradevisor.job.publish;

import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.telegram.BotMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MapSignal2Message {

    public static BotMessage from(Signals signal) {
        return new BotMessage(
                "title",
                "text",
                "image".getBytes(StandardCharsets.UTF_8),
                List.of()
        );
    }
}
