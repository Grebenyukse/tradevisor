package ru.grnk.tradevisor.integration.telegram.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("${app.integration.telegram.webhook-path}")
@RequiredArgsConstructor
public class WebhookController {

    private final TelegramBotService telegramBotService;

    @PostMapping
    public void handleWebhook(@RequestBody Update update) {
        log.info("Received update: {}", update.getUpdateId());
        telegramBotService.processUpdate(update);
    }

    @GetMapping
    public void handleWebhookInfo() {
        log.info("Received get info: {}", telegramBotService.getWebhookInfo());
    }

    @PutMapping()
    public void setWebHook() {
        log.info("set webhook: {}", telegramBotService.setWebhook());
    }

    @DeleteMapping
    public void deleteWebhook() {
        log.info("deleted webhook: {}", telegramBotService.deleteWebhook());
    }

}
