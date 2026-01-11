package ru.grnk.tradevisor.collect.utils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public class MarketDataValidator {

    /**
     * Проверяет, является ли actualTime валидной датой.
     * Дата считается валидной, если она не отстает от последнего рабочего часа более чем на 6 часов.
     *
     * @param actualTime дата для проверки
     * @return true, если дата валидна, иначе false
     */
    public static boolean isActualTimeValid(OffsetDateTime actualTime) {
        if (actualTime == null) {
            return false;
        }

        OffsetDateTime referenceTime = getReferenceTime(actualTime);
        Duration duration = Duration.between(actualTime, referenceTime);

        // Если разница больше 6 часов (360 минут), то невалидно
        return !duration.isNegative() && duration.toHours() <= 6;
    }

    /**
     * Определяет эталонное время для сравнения.
     *
     * @param currentTime используется для определения зоны и времени
     * @return эталонное время
     */
    private static OffsetDateTime getReferenceTime(OffsetDateTime currentTime) {
        LocalDateTime localNow = currentTime.toLocalDateTime();
        DayOfWeek dayOfWeek = localNow.getDayOfWeek();

        switch (dayOfWeek) {
            case SATURDAY:
            case SUNDAY:
                // Найти ближайшую прошедшую пятницу в 23:59
                LocalDateTime lastFriday = localNow.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY))
                        .with(LocalTime.of(23, 59));
                return lastFriday.atOffset(currentTime.getOffset());
            default:
                // Будний день — использовать текущее время
                return currentTime;
        }
    }
}
