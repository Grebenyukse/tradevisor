package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentInfoResponse {
    private InstrumentResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstrumentResult {
        private List<Instrument> list;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Instrument {
        private PriceFilter priceFilter;
        private LotSizeFilter lotSizeFilter;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceFilter {
        private String tickSize;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LotSizeFilter {
        private String minOrderQty;
    }
}
