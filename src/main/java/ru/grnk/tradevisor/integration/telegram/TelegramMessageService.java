package ru.grnk.tradevisor.integration.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.integration.telegram.sender.TelegramSender;

@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramSender telegramSender;

    public void sendMessage(String url, String title, String text, Integer id, Integer threadId) {
        telegramSender.sendMessage(url, title, text, id, threadId);
    }

}
