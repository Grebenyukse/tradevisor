package ru.grnk.tradevisor.trade;

import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;

import java.util.Comparator;

class SignalComparator implements Comparator<Signals> {
    @Override
    public int compare(Signals s1, Signals s2) {
        // Сначала сравниваем по статусу (EXECUTED имеет наивысший приоритет)
        int statusComparison = Integer.compare(getStatusPriority(s1.getStatus()), getStatusPriority(s2.getStatus()));
        if (statusComparison != 0) {
            return statusComparison;
        }
        // Если статусы равны, сравниваем по дате создания (более старые первыми)
        return s1.getCreatedAt().compareTo(s2.getCreatedAt());
    }

    private int getStatusPriority(String status) {
        switch (status) {
            case "EXECUTED":
                return 0;
            case "CONFIRMED":
                return 1;
            case "PUBLISHED":
                return 2;
            case "CREATED":
                return 3;
            default:
                return 999;
        }
    }
}
