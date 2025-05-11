package ru.grnk.tradevisor.integration.ai;

public interface AskAiModel {
    String ask(String prompt, Integer attempts);

    String source();

}
