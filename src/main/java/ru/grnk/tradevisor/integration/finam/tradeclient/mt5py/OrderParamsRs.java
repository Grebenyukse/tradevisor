package ru.grnk.tradevisor.integration.finam.tradeclient.mt5py;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OrderParamsRs(@JsonProperty("orders") List<Order> orders) {

    public static record Order(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("direction") int direction,
            @JsonProperty("lot") double lot,
            @JsonProperty("price_open") double priceOpen,
            @JsonProperty("sl") double sl,
            @JsonProperty("tp") double tp,
            @JsonProperty("executed") boolean executed
    ) {
        @Override
        public String toString() {
            return "Order{" +
                    "symbol='" + symbol + '\'' +
                    ", direction=" + direction +
                    ", lot=" + lot +
                    ", priceOpen=" + priceOpen +
                    ", sl=" + sl +
                    ", tp=" + tp +
                    ", executed=" + executed +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OrderParamsRs{" +
                "orders=" + orders +
                '}';
    }
}

