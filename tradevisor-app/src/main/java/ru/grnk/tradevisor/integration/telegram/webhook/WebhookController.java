package ru.grnk.tradevisor.integration.telegram.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("${app.integration.telegram.webhook-path}")
public class WebhookController {

    private final TelegramBotService telegramBotService;

    public WebhookController(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostMapping
    public void handleWebhook(@RequestBody Update update) {
        log.info("Received update: {}", update.getUpdateId());
        telegramBotService.processUpdate(update);
    }

}
