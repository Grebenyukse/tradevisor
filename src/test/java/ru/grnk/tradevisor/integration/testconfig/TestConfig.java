package ru.grnk.tradevisor.integration.testconfig;

public final class TestConfig {
    private TestConfig() {}

    public static String telegramBotToken() {
        String token = System.getenv("CHAT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN must be set");
        }
        return token;
    }

    public static String telegramApiBase() {
        return "https://api.telegram.org/bot" + telegramBotToken();
    }

    public static String cloImage() {
        return System.getenv().getOrDefault("CLO_IMAGE", "ghcr.io/abiosoft/clo:latest");
    }

    public static int cloHttpPort() {
        return Integer.parseInt(System.getenv().getOrDefault("CLO_HTTP_PORT", "4040"));
    }

    public static int botPort() {
        return Integer.parseInt(System.getenv().getOrDefault("BOT_PORT", "8080"));
    }

    public static String botHost() {
        return System.getenv().getOrDefault("BOT_HOST", "host.docker.internal");
    }
}
