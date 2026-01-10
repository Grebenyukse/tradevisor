package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;

public class CandleDeserializer extends JsonDeserializer<BybitCandlesResponse.Candle> {

    @Override
    public BybitCandlesResponse.Candle deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        long openTime = node.get(0).asLong();
        BigDecimal openPrice = new BigDecimal(node.get(1).asText());
        BigDecimal highPrice = new BigDecimal(node.get(2).asText());
        BigDecimal lowPrice = new BigDecimal(node.get(3).asText());
        BigDecimal closePrice = new BigDecimal(node.get(4).asText());
        BigDecimal volume = new BigDecimal(node.get(5).asText());
        BigDecimal turnover = new BigDecimal(node.get(6).asText());
        return new BybitCandlesResponse.Candle(openTime, openPrice, highPrice, lowPrice, closePrice, volume, turnover);
    }
}
