package ru.grnk.tradevisor.acollect;

import java.util.List;

public interface TrvCollector <T> {
    List<T> collect();

    default String source() {
        return "trvAiModel";
    };
}
