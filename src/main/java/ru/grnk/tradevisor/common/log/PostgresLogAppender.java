package ru.grnk.tradevisor.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.SneakyThrows;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class PostgresLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final String sql = "INSERT INTO tradevisor.log_table (log_level, log_date, message, logger_name, thread_name, stack_trace) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    private Connection connection;
    private String dbUrl;
    private String username;
    private String password;

    public PostgresLogAppender() {
        super();
        init();
    }

    private void init() {
        this.dbUrl = findSysVar("SPRING_DATASOURCE_URL");
        this.username = findSysVar("SPRING_DATASOURCE_USERNAME");
        this.password = findSysVar("SPRING_DATASOURCE_PASSWORD_V2");
    }

    private static String findSysVar(String key) {
        return System.getenv(key) != null ? System.getenv(key) : System.getProperty(key);
    }

    @SneakyThrows
    private Connection createConnection() {
        if (dbUrl == null || username == null || password == null) {
            init();
            return null;
        }
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        return DriverManager.getConnection(dbUrl, props);
    }

    @Override
    protected void append(ILoggingEvent event) {
        String name = event.getLoggerName();
        if (name.startsWith("org.springframework") ||
                name.startsWith("com.zaxxer.hikari") ||
                name.startsWith("org.hibernate") ||
                name.startsWith("java.sql") ||
                name.startsWith("ru.grnk.tradevisor.common.log")) { // Игнорируем сам логгер!
            return;
        }
        saveToDatabase(event);
    }

    private void saveToDatabase(ILoggingEvent event) {
        try {
            var logEntry = new LogEntry(
                    event.getLevel().toString(),
                    LocalDateTime.now(),
                    event.getFormattedMessage(),
                    event.getLoggerName(),
                    event.getThreadName(),
                    getFullStackTrace(event.getThrowableProxy())
            );
            log(logEntry);
        } catch (Exception ignored) {
            String err = ignored.getMessage();
        }
    }

    private String getFullStackTrace(ch.qos.logback.classic.spi.IThrowableProxy throwableProxy) {
        if (throwableProxy == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append("\n");

        ch.qos.logback.classic.spi.StackTraceElementProxy[] stackTraceElements =
                throwableProxy.getStackTraceElementProxyArray();
        if (stackTraceElements != null) {
            for (ch.qos.logback.classic.spi.StackTraceElementProxy element : stackTraceElements) {
                sb.append("\tat ").append(element.getSTEAsString()).append("\n");
            }
        }

        return sb.toString();
    }

    public void log(LogEntry logEntry) {
        if (connection == null) {
            connection = createConnection();
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, logEntry.getLogLevel());
            stmt.setTimestamp(2, Timestamp.valueOf(logEntry.getLogDate()));
            stmt.setString(3, logEntry.getMessage());
            stmt.setString(4, logEntry.getLoggerName());
            stmt.setString(5, logEntry.getThreadName());
            stmt.setString(6, logEntry.getStackTrace());
            stmt.executeUpdate();
        } catch (SQLException e) {
            String msg = e.getMessage();
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                String msg =  e.getMessage();
            }
        }
    }
}
