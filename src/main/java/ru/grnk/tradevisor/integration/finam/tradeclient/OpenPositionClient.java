package ru.grnk.tradevisor.integration.finam.tradeclient;

public interface OpenPositionClient {

    boolean openPosition(String symbol, float priceOpen, float stopLoss, float takeProfit, int direction, int signalId);

}
