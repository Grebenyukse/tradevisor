package ru.grnk.tradevisor.eintegration.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@Slf4j
public class TelegramFacade {

    public TelegramFacade() {
    }

    public SendMessage handleUpdate(Update update) {

        return new SendMessage();
    }

    private SendMessage handleInputMessage(Message message) {

        return new SendMessage();
    }


}
