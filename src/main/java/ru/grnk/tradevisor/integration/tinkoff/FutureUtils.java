package ru.grnk.tradevisor.integration.tinkoff;

import java.time.Year;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

public class FutureUtils {

    /**
     * Проверяет, является ли фьючерсный код ближайшим торгуемым или уже истекшим
     * @param futureCode код фьючерса в формате XXMY, где:
     *                   XX - код инструмента
     *                   M - код квартала (H=3й квартал, M=6й квартал, U=9й квартал, Z=12й квартал)
     *                   Y - последняя цифра года
     * @return true если фьючерс еще торгуется и является ближайшим, false если уже истек
     */
    public static boolean isNearestFutureCode(String futureCode) {
        if (futureCode == null || futureCode.length() < 3) {
            return false;
        }

        try {
            // Извлекаем код квартала и год из последних двух символов
            char quarterCode = futureCode.charAt(futureCode.length() - 2);
            char yearDigit = futureCode.charAt(futureCode.length() - 1);

            // Определяем месяц экспирации по коду квартала
            int expirationMonth = getQuarterEndMonth(quarterCode);
            if (expirationMonth == -1) {
                return false;
            }

            // Определяем год экспирации
            int currentYear = Year.now().getValue();
            int currentYearLastDigit = currentYear % 10;
            int expirationYear;

            // Определяем полный год экспирации
            if (Character.isDigit(yearDigit)) {
                int targetYearDigit = Character.getNumericValue(yearDigit);

                // Если цифра года меньше текущей, значит год следующего десятилетия
                if (targetYearDigit < currentYearLastDigit) {
                    expirationYear = ((currentYear / 10) * 10) + 10 + targetYearDigit;
                } else {
                    expirationYear = ((currentYear / 10) * 10) + targetYearDigit;
                }
            } else {
                return false;
            }

            // Дата экспирации - третий пятница месяца (типичная дата экспирации фьючерсов)
            LocalDate expirationDate = getThirdFriday(expirationYear, expirationMonth);

            // Текущая дата
            LocalDate currentDate = LocalDate.now();

            // Фьючерс актуален, если дата экспирации еще не наступила или сегодня последний торговый день
            return !currentDate.isAfter(expirationDate);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Возвращает номер месяца по коду квартала
     * @param quarterCode код квартала (H, M, U, Z)
     * @return номер месяца (3, 6, 9, 12) или -1 если код неверный
     */
    private static int getQuarterEndMonth(char quarterCode) {
        return switch (quarterCode) {
            case 'H' -> 3;  // 1й квартал заканчивается в марте
            case 'M' -> 6;  // 2й квартал заканчивается в июне
            case 'U' -> 9;  // 3й квартал заканчивается в сентябре
            case 'Z' -> 12; // 4й квартал заканчивается в декабре
            default -> -1;
        };
    }

    /**
     * Находит третий пятницу месяца (стандартная дата экспирации фьючерсов)
     * @param year год
     * @param month месяц
     * @return дата третьей пятницы месяца
     */
    private static LocalDate getThirdFriday(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDayOfMonth = yearMonth.atDay(1);

        // Находим первую пятницу месяца
        LocalDate firstFriday = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.FRIDAY));

        // Третья пятница = первая пятница + 14 дней
        return firstFriday.plusDays(14);
    }

    /**
     * Альтернативная реализация для проверки, является ли фьючерс ближайшим среди активных
     * @param futureCode код фьючерса для проверки
     * @param allActiveFutures список всех активных фьючерсов
     * @return true если это ближайший фьючерс
     */
    private static boolean isNearestFutureCode(String futureCode, java.util.List<String> allActiveFutures) {
        if (futureCode == null || allActiveFutures == null || !allActiveFutures.contains(futureCode)) {
            return false;
        }

        LocalDate targetExpiration = getExpirationDate(futureCode);
        if (targetExpiration == null) {
            return false;
        }

        LocalDate currentDate = LocalDate.now();
        LocalDate nearestExpiration = null;
        String nearestFutureCode = null;

        // Ищем фьючерс с ближайшей датой экспирации среди активных
        for (String future : allActiveFutures) {
            LocalDate expiration = getExpirationDate(future);
            if (expiration != null && !expiration.isBefore(currentDate)) { // Только будущие фьючерсы
                if (nearestExpiration == null || expiration.isBefore(nearestExpiration)) {
                    nearestExpiration = expiration;
                    nearestFutureCode = future;
                }
            }
        }

        return futureCode.equals(nearestFutureCode);
    }

    /**
     * Вспомогательный метод для получения даты экспирации по коду фьючерса
     * @param futureCode код фьючерса
     * @return дата экспирации или null если ошибка
     */
    private static LocalDate getExpirationDate(String futureCode) {
        if (futureCode == null || futureCode.length() < 3) {
            return null;
        }

        try {
            char quarterCode = futureCode.charAt(futureCode.length() - 2);
            char yearDigit = futureCode.charAt(futureCode.length() - 1);

            int expirationMonth = getQuarterEndMonth(quarterCode);
            if (expirationMonth == -1) {
                return null;
            }

            int currentYear = Year.now().getValue();
            int currentYearLastDigit = currentYear % 10;
            int expirationYear;

            if (Character.isDigit(yearDigit)) {
                int targetYearDigit = Character.getNumericValue(yearDigit);
                if (targetYearDigit < currentYearLastDigit) {
                    expirationYear = ((currentYear / 10) * 10) + 10 + targetYearDigit;
                } else {
                    expirationYear = ((currentYear / 10) * 10) + targetYearDigit;
                }
            } else {
                return null;
            }

            return getThirdFriday(expirationYear, expirationMonth);
        } catch (Exception e) {
            return null;
        }
    }

}
