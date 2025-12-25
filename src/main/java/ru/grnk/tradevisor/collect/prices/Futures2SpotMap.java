package ru.grnk.tradevisor.collect.prices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Futures2SpotMap {
    public static final Set<String> FINAM_PRELOAD_EXCHANGES_MIC = Set.of("MISX", "XNYS", "XHKG");

    public static final String SKIP_TICKER_MAPPING_VALUE = "NOT_FOR_TRADE";

    public static final String JOIN_ENDLESS_FUTURE_MAPPING_VALUE = "JOIN_FUTURES";
    public static final Map<String, String> FUTURES_2_SPOT = new HashMap<String, String>() {{
        put("IBIT", "BTCUSDT@bybit");
        put("BTC", "BTCUSDT@bybit");
        put("ETHA", "ETHUSDT@bybit");
        put("ETH", "ETHUSDT@bybit");
        put("AUDU", "AUDUSD=X");
        put("GBPU", "GBPUSD=X");
        put("ED", "EURUSD=X");
        put("Eu", "EURRUB=X");
        put("AED", "RUB=X");
        put("EURRUBF Евро - Рубль", "EURRUB=X");
        put("INR", "INRRUB=X");
        put("R2000", "^RUT");
        put("BRAZIL", "EWZ");
        put("DAX", "^GDAXI");
        put("STOX", "^STOXX50E");
        put("DJ30", "^DJI");
        put("NASD", "^IXIC");
        put("HANG", "^HSI");
        put("CHINA", "MCHI");
        put("NIKK", "^N225");
        put("SOXQ", "SOXQ");
        put("SPYF", "SPY");
        put("TLT", "TLT");
        put("ALUM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("GOLD", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("GLDRUBF Золото (rub)", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("SILV", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("SILVM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("COPPER", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("NICKEL", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("ORANGE", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("SUGR", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("OGI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("FNI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("COFFEE", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("RVI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("IPO", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("ZINC", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("AFRICA", SKIP_TICKER_MAPPING_VALUE);
        put("RGBI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("MMI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("EM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("WHEAT", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("SUGAR", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("NGM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("NG", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("RTSM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("SAUDI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("COCOA", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("MIX", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("INDIA", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("CNI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("ARGT", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("TTF", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("PLD", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("BR", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("RTS", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("PLT", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("MOEXCNY", SKIP_TICKER_MAPPING_VALUE);
        put("IMOEXF Индекс МосБиржи", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("GL", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("BRM", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("HOME", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
        put("MXI", JOIN_ENDLESS_FUTURE_MAPPING_VALUE);
    }};
}
