package ru.grnk.tradevisor.integration.telegram.webhook.handler.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;
import ru.grnk.tradevisor.integration.telegram.dto.TelegramMessageBuilder;

@Service
@RequiredArgsConstructor
public class DeleteMessagesCmdHandlerImpl implements TgMessageHandler {

    private final TelegramApiClient telegramApiClient;

    @Override
    public String command() {
        return "/delete_all_messages";
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.getChatId();

        // Отправляем "очищающее" сообщение
        SendMessage clearMessage = new SendMessage();
        clearMessage.setChatId(chatId.toString());
        clearMessage.setText(createClearText());

        telegramApiClient.sendMessage(clearMessage);

        // Отправляем подтверждающее сообщение поверх
        telegramApiClient.sendMessage(
                TelegramMessageBuilder
                        .sendSimpleMessage(chatId, "Чат очищен")
        );
    }

    /**
     * Создаёт длинный текст из множества переносов строк и пробелов,
     * чтобы затереть предыдущие сообщения.
     */
    private String createClearText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) { // Можно увеличить до нужного уровня
            sb.append("\n\n\n\n\n\n\n\n\n\n"); // 10 новых строк за итерацию
        }
        return sb.toString();
    }
}
