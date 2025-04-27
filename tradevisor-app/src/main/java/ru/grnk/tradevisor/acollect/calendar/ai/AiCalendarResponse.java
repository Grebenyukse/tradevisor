package ru.grnk.tradevisor.acollect.calendar.ai;

import java.util.ArrayList;

public record AiCalendarResponse(
        Integer impact,
        String country,
        ArrayList<String> tickers,
        String title,
        String description,
        String event_date) {

}