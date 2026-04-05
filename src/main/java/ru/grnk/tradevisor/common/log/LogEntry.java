package ru.grnk.tradevisor.common.log;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LogEntry {
    private Long id;
    private String logLevel;
    private LocalDateTime logDate;
    private String message;
    private String loggerName;
    private String threadName;
    private String stackTrace;

    public LogEntry(String logLevel, LocalDateTime logDate, String message,
                    String loggerName, String threadName, String stackTrace) {
        this.logLevel = logLevel;
        this.logDate = logDate;
        this.message = message;
        this.loggerName = loggerName;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
    }
}