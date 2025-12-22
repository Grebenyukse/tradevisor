package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenOrdersResponse {
    private OrderResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderResult {
        private List<Order> list;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        private String side;
        private String price;
        private String qty;
        private String orderStatus;
        private String symbol;
        private String timeInForce;
    }
}
