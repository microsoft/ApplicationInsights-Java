package com.microsoft.applicationinsights;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class FormattedTime {

    public static String fromNow() {
        return fromEpochMillis(System.currentTimeMillis());
    }

    public static String fromDate(Date date) {
        return fromEpochMillis(date.getTime());
    }

    public static String fromEpochNanos(long epochNanos) {
        return Instant.ofEpochMilli(NANOSECONDS.toMillis(epochNanos))
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static String fromEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
