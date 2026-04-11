package ru.grnk.tradevisor.integration.telegram.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.grnk.tradevisor.integration.telegram.api.TelegramApiClient;
import ru.grnk.tradevisor.integration.telegram.webhook.handler.TelegramBotConsumer;

@Slf4j
@RestController
@RequestMapping("${app.integration.telegram.webhook-path}")
@RequiredArgsConstructor
public class WebhookController {

    private final TelegramBotConsumer telegramBotConsumer;
    private final TelegramApiClient telegramApiClient;

    @PostMapping
    public void handleWebhook(@RequestBody Update update) {
        telegramBotConsumer.processUpdate(update);
    }

    @GetMapping
    public void handleWebhookInfo() {
        log.info("Received get info: {}", telegramApiClient.getWebhookInfo());
    }

    @PutMapping()
    public void setWebHook() {
        log.info("set webhook: {}", telegramApiClient.setWebhook());
    }

    @DeleteMapping
    public void deleteWebhook() {
        log.info("deleted webhook: {}", telegramApiClient.deleteWebhook());
    }

}
