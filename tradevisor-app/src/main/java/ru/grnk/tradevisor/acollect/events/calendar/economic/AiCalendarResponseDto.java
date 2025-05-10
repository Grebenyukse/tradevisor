package ru.grnk.tradevisor.acollect.events.calendar.economic;

import java.util.ArrayList;

public record AiCalendarResponseDto(
        Integer impact,
        String country,
        ArrayList<String> tickers,
        String title,
        String description,
        String event_date) {

}