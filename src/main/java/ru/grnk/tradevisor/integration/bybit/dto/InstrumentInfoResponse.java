package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentInfoResponse {
    private int retCode;
    private String retMsg;
    private InstrumentResult result;
    private RetExtInfo retExtInfo;
    private long time;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstrumentResult {
        private String category;
        private List<Instrument> list;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Instrument {
        private String symbol;
        private String baseCoin;
        private String quoteCoin;
        private String innovation;
        private String status;
        private String marginTrading;
        private String stTag;
        private LotSizeFilter lotSizeFilter;
        private PriceFilter priceFilter;
        private RiskParameters riskParameters;
        private String symbolType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LotSizeFilter {
        private String basePrecision;
        private String quotePrecision;
        private String minOrderQty;
        private String maxOrderQty;
        private String minOrderAmt;
        private String maxOrderAmt;
        private String maxLimitOrderQty;
        private String maxMarketOrderQty;
        private String postOnlyMaxLimitOrderSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceFilter {
        private String tickSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RiskParameters {
        private String priceLimitRatioX;
        private String priceLimitRatioY;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetExtInfo {
        // Пока пустой, но можно расширить при необходимости
    }
}

