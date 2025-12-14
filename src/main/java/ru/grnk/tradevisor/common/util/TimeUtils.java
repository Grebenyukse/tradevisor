package ru.grnk.tradevisor.common.util;

import com.google.protobuf.Timestamp;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public class TimeUtils {

    public static Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
        var instant = offsetDateTime.toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Timestamp convertToTimestamp(ZonedDateTime zonedDateTime) {
        var instant = zonedDateTime.toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
