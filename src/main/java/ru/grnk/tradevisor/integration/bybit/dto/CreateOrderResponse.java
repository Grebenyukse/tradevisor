package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderResponse {
    private CreateOrderResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateOrderResult {
        private String orderId;
    }
}
