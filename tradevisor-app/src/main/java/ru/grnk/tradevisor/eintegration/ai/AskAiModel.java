package ru.grnk.tradevisor.eintegration.ai;

public interface AskAiModel {
    String ask(String prompt, Integer attempts);

}
