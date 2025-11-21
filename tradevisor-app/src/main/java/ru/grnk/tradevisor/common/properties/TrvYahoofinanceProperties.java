package ru.grnk.tradevisor.common.properties;

public record TrvYahoofinanceProperties(String url,
                                        Boolean enabled,
                                        Integer historyMaxDepthDays) {

}
