package ru.grnk.tradevisor.collect.events.economic;

import java.util.ArrayList;

public record AiCalendarResponseDto(
        Integer impact,
        String country,
        ArrayList<String> tickers,
        String title,
        String description,
        String event_date) {

}