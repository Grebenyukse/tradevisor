package ru.grnk.tradevisor.notify.publish;

public interface NotificationService {
    void sendMessage(String url, String title, String text, Integer id, Integer threadId);
}
