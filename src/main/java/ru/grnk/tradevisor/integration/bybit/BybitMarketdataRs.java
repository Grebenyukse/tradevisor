package ru.grnk.tradevisor.integration.bybit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public record BybitMarketdataRs(
        int retCode,
        String retMsg,
        Result result,
        RetExtInfo retExtInfo,
        long time
) {

    public record Result(
            String category,
            String symbol,
            @JsonProperty("list")
            @JsonDeserialize(contentUsing = CandlestickDeserializer.class)
            List<Candlestick> list
    ) {}

    public record Candlestick(
            long openTime,         // Время открытия свечи в миллисекундах
            float openPrice,   // Цена открытия
            float highPrice,   // Максимальная цена
            float lowPrice,    // Минимальная цена
            float closePrice,  // Цена закрытия
            BigDecimal volume,      // Объем торгов
            BigDecimal turnover     // Оборот
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Candlestick fromList(List<String> data) {
            return new Candlestick(
                    Long.parseLong(data.get(0)),       // openTime
                    Float.parseFloat(data.get(1)),       // openPrice
                    Float.parseFloat(data.get(2)),       // highPrice
                    Float.parseFloat(data.get(3)),       // lowPrice
                    Float.parseFloat(data.get(4)),       // closePrice
                    new BigDecimal(data.get(5)),       // volume
                    new BigDecimal(data.get(6))        // turnover
            );
        }
    }

    public static class CandlestickDeserializer extends JsonDeserializer<Candlestick> {
        @Override
        public Candlestick deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            List<String> data = p.readValueAs(new TypeReference<List<String>>() {});
            return Candlestick.fromList(data);
        }
    }

    public record RetExtInfo() {}
}
