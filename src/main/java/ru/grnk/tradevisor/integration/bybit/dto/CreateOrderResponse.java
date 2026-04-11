package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CreateOrderResponse {
    private CreateOrderResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateOrderResult {
        private String orderId;
    }
}
