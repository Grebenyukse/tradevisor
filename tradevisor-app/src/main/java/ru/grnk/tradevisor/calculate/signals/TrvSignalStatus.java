package ru.grnk.tradevisor.calculate.signals;

/**
 * CREATED - вновь создан на основании рыночных данных
 * PUBLISHED - опубликован в telegram или через другое средство нотификации
 * EXPIRED - сигнал потерял актуальность по времени, обрабатывать его больше не нужно.
 * EXECUTED - по сигналу выставлена торговая позиция
 * CANCELLED - сигнал отменен из другого модуля или получен новый сигнал, который отменяет этот.
 */
public enum TrvSignalStatus {
    CREATED,
    PUBLISHED,
    EXPIRED,
    EXECUTED,
    CANCELLED
}
