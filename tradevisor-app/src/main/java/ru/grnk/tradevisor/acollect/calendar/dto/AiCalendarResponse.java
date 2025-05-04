package ru.grnk.tradevisor.acollect.calendar.dto;

import java.util.ArrayList;

public record AiCalendarResponse(
        Integer impact,
        String country,
        ArrayList<String> tickers,
        String title,
        String description,
        String event_date) {

}