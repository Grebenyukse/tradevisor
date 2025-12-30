package ru.grnk.tradevisor.integration.finam.tradeclient.mt5py;

public record OpenPositionRs(
        Integer buy_limit_order_id,
        Integer sell_limit_order_id,
        Integer stop_loss_order_id,
        Integer take_profit_order_id,
        Double lot,
        Integer direction,
        String symbol,
        String created_at,
        String status
) {
    @Override
    public String toString() {
        return "open position result {" +
                "buy_limit_order_id=" + buy_limit_order_id +
                ", sell_limit_order_id=" + sell_limit_order_id +
                ", stop_loss_order_id=" + stop_loss_order_id +
                ", take_profit_order_id=" + take_profit_order_id +
                ", lot=" + lot +
                ", direction=" + direction +
                ", symbol='" + symbol + '\'' +
                ", created_at='" + created_at + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}