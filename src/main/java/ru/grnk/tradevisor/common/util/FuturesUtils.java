package ru.grnk.tradevisor.common.util;

import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuturesUtils {

    // Паттерн для фьючерсов формата XXMY (например: BRM6, CLZ3)
    private static final Pattern FUTURES_PATTERN = Pattern.compile("^([A-Z]{2})([A-Z])(\\d)$");

    // Минимальное количество недель до экспирации
    private static final int MIN_WEEKS_TO_EXPIRATION = 3;

    /**
     * Проверяет, является ли фьючерс актуальным (до экспирации не меньше 3 недель)
     *
     * @param ticker тикер фьючерса
     * @return true если фьючерс актуален, false otherwise
     */
    public static boolean isFuturesActual(Tickers ticker) {
        return isFuturesActual(ticker.getTickerCode(), LocalDate.now());
    }

    /**
     * Проверяет, является ли фьючерс актуальным (до экспирации не меньше 3 недель)
     *
     * @param tickerCode код тикера фьючерса
     * @return true если фьючерс актуален, false otherwise
     */
    public static boolean isFuturesActual(String tickerCode) {
        return isFuturesActual(tickerCode, LocalDate.now());
    }

    /**
     * Проверяет, является ли фьючерс актуальным на заданную дату
     *
     * @param tickerCode код тикера фьючерса
     * @param currentDate текущая дата для сравнения
     * @return true если фьючерс актуален, false otherwise
     */
    public static boolean isFuturesActual(String tickerCode, LocalDate currentDate) {
        Optional<LocalDate> expirationDate = extractExpirationDate(tickerCode);
        return expirationDate.map(date ->
                        java.time.temporal.ChronoUnit.WEEKS.between(currentDate, date) >= MIN_WEEKS_TO_EXPIRATION)
                .orElse(false);
    }

    /**
     * Извлекает дату экспирации из кода фьючерса
     * Формат: XXMY где M - буква месяца, Y - цифра года (0-9)
     *
     * @param tickerCode код тикера фьючерса
     * @return Optional с датой экспирации или пустой Optional если не удалось распарсить
     */
    public static Optional<LocalDate> extractExpirationDate(String tickerCode) {
        if (tickerCode == null || tickerCode.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = FUTURES_PATTERN.matcher(tickerCode);
        if (matcher.matches()) {
            String monthLetter = matcher.group(2);
            String yearDigit = matcher.group(3);

            int month = getMonthFromLetter(monthLetter);
            if (month == -1) {
                return Optional.empty();
            }

            int year = calculateYearFromDigit(yearDigit);

            // Экспирация - третья пятница месяца
            return Optional.of(getThirdFriday(year, month));
        }

        return Optional.empty();
    }

    /**
     * Вычисляет полный год из одной цифры
     * Логика: если цифра соответствует будущему году относительно текущего, используем её,
     * иначе добавляем 10 лет
     *
     * @param yearDigit цифра года (0-9)
     * @return полный год
     */
    private static int calculateYearFromDigit(String yearDigit) {
        int currentYear = LocalDate.now().getYear();
        int currentLastDigit = currentYear % 10;

        int targetYearLastDigit = Integer.parseInt(yearDigit);
        int baseYear = (currentYear / 10) * 10 + targetYearLastDigit;

        // Если полученный год уже прошёл или текущий, но мы хотим будущие контракты
        if (baseYear < currentYear) {
            baseYear += 10;
        } else if (baseYear == currentYear) {
            // Проверяем, не прошёл ли этот год уже
            if (LocalDate.now().getMonthValue() > 6) { // Упрощенная логика
                baseYear += 10;
            }
        }

        return baseYear;
    }

    /**
     * Получает дату третьей пятницы месяца (стандартная дата экспирации фьючерсов)
     *
     * @param year год
     * @param month месяц (1-12)
     * @return дата третьей пятницы месяца
     */
    private static LocalDate getThirdFriday(int year, int month) {
        // Первая пятница месяца
        LocalDate firstFriday = LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.FRIDAY));

        // Третья пятница = первая пятница + 14 дней
        return firstFriday.plusDays(14);
    }

    /**
     * Преобразует букву месяца в номер месяца согласно стандартам фьючерсов
     *
     * @param letter буква месяца (F, G, H, J, K, M, N, Q, U, V, X, Z)
     * @return номер месяца или -1 если буква не распознана
     */
    private static int getMonthFromLetter(String letter) {
        return switch (letter.toUpperCase()) {
            case "F" -> 1;  // Январь
            case "G" -> 2;  // Февраль
            case "H" -> 3;  // Март
            case "J" -> 4;  // Апрель
            case "K" -> 5;  // Май
            case "M" -> 6;  // Июнь
            case "N" -> 7;  // Июль
            case "Q" -> 8;  // Август
            case "U" -> 9;  // Сентябрь
            case "V" -> 10; // Октябрь
            case "X" -> 11; // Ноябрь
            case "Z" -> 12; // Декабрь
            default -> -1;
        };
    }

    /**
     * Компаратор для сортировки фьючерсов по дате экспирации от ближайшего к дальнему
     */
    public static final java.util.Comparator<Tickers> EXPIRATION_DATE_COMPARATOR =
            (ticker1, ticker2) -> {
                Optional<LocalDate> expDate1 = extractExpirationDate(ticker1.getTickerCode());
                Optional<LocalDate> expDate2 = extractExpirationDate(ticker2.getTickerCode());

                if (expDate1.isPresent() && expDate2.isPresent()) {
                    return expDate1.get().compareTo(expDate2.get());
                } else if (expDate1.isPresent()) {
                    return -1; // Первый имеет дату, второй нет - первый "меньше"
                } else if (expDate2.isPresent()) {
                    return 1;  // Второй имеет дату, первый нет - первый "больше"
                } else {
                    return 0;  // Оба не имеют даты - равны
                }
            };

    /**
     * Возвращает минимальное количество недель до экспирации для актуального фьючерса
     *
     * @return минимальное количество недель
     */
    public static int getMinWeeksToExpiration() {
        return MIN_WEEKS_TO_EXPIRATION;
    }
}
