package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionResponse {
    private PositionResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PositionResult {
        private List<Position> list;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Position {
        private String avgPrice;
        private String size;
        private String side;
    }
}
