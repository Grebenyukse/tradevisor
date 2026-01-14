package ru.grnk.tradevisor.integration.finam.tradeclient.mt5py;

import lombok.Builder;

@Builder
public record OpenPositionRq(
        String symbol,
        Price price_open,
        Price stop_loss,
        Price take_profit,
        int direction,
        int quantity
) {
}
