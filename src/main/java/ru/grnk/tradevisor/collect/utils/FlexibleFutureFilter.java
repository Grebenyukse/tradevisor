package ru.grnk.tradevisor.collect.utils;

import java.util.Set;

public class FlexibleFutureFilter {
    
    private final static Set<Character> allowedMonths = Set.of('H', 'M', 'U', 'Z');
    
    public static boolean isAllowedFuture(String future) {
        if (future == null || future.length() < 2) {
            return false;
        }
        
        char monthChar = future.charAt(future.length() - 2);
        return allowedMonths.contains(monthChar);
    }
}
