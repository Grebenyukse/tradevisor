package ru.grnk.tradevisor.acollect.calendar.ai;

public class TrvCalendarPrompt {

    public static final String TRV_CALENDAR_PROMPT = """
            Use www.investing.com/economic-calendar. Return JSON array with objects containing: impact (1-3),
            country (eur/usa/china/russia), tickers[], title, description (<10 words), event_date (YYYY-MM-DD).
            Dates: %s to %s. Example: [{"impact":3,"country":"usa","tickers":["DXY"],"title":"Nonfarm Payrolls",
            "description":"Monthly jobs report", "event_date": "2025-05-06"}]
            """;

}
