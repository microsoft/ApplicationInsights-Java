package com.microsoft.applicationinsights;

import static java.util.concurrent.TimeUnit.*;

public class FormattedDuration {

    private static final long NANOSECONDS_PER_DAY = DAYS.toNanos(1);
    private static final long NANOSECONDS_PER_HOUR = HOURS.toNanos(1);
    private static final long NANOSECONDS_PER_MINUTE = MINUTES.toNanos(1);
    private static final long NANOSECONDS_PER_SECOND = SECONDS.toNanos(1);

    private static final long MILLISECONDS_PER_DAY = DAYS.toMillis(1);
    private static final long MILLISECONDS_PER_HOUR = HOURS.toMillis(1);
    private static final long MILLISECONDS_PER_MINUTE = MINUTES.toMillis(1);
    private static final long MILLISECONDS_PER_SECOND = SECONDS.toMillis(1);

    private static final ThreadLocal<StringBuilder> reusableStringBuilder = ThreadLocal.withInitial(StringBuilder::new);

    public static String fromNanos(long durationNanos) {
        long remainingNanos = durationNanos;

        long days = remainingNanos / NANOSECONDS_PER_DAY;
        remainingNanos = remainingNanos % NANOSECONDS_PER_DAY;

        long hours = remainingNanos / NANOSECONDS_PER_HOUR;
        remainingNanos = remainingNanos % NANOSECONDS_PER_HOUR;

        long minutes = remainingNanos / NANOSECONDS_PER_MINUTE;
        remainingNanos = remainingNanos % NANOSECONDS_PER_MINUTE;

        long seconds = remainingNanos / NANOSECONDS_PER_SECOND;
        remainingNanos = remainingNanos % NANOSECONDS_PER_SECOND;

        // TODO (trask) even better than reusing string builder would be to write this directly to json stream
        //  during json serialization
        StringBuilder sb = reusableStringBuilder.get();
        sb.setLength(0);

        appendDaysHoursMinutesSeconds(sb, days, hours, minutes, seconds);
        appendMinSixDigits(sb, NANOSECONDS.toMicros(remainingNanos));

        return sb.toString();
    }

    public static String fromMillis(long durationMillis) {
        long remainingMillis = durationMillis;

        long days = remainingMillis / MILLISECONDS_PER_DAY;
        remainingMillis = remainingMillis % MILLISECONDS_PER_DAY;

        long hours = remainingMillis / MILLISECONDS_PER_HOUR;
        remainingMillis = remainingMillis % MILLISECONDS_PER_HOUR;

        long minutes = remainingMillis / MILLISECONDS_PER_MINUTE;
        remainingMillis = remainingMillis % MILLISECONDS_PER_MINUTE;

        long seconds = remainingMillis / MILLISECONDS_PER_SECOND;
        remainingMillis = remainingMillis % MILLISECONDS_PER_SECOND;

        // TODO (trask) even better than reusing string builder would be to write this directly to json stream
        //  during json serialization
        StringBuilder sb = reusableStringBuilder.get();
        sb.setLength(0);

        appendDaysHoursMinutesSeconds(sb, days, hours, minutes, seconds);
        appendMinThreeDigits(sb, remainingMillis);
        sb.append("000");

        return sb.toString();
    }

    private static void appendDaysHoursMinutesSeconds(StringBuilder sb, long days, long hours, long minutes, long seconds) {
        if (days > 0) {
            sb.append(days);
            sb.append('.');
        }
        appendMinTwoDigits(sb, hours);
        sb.append(':');
        appendMinTwoDigits(sb, minutes);
        sb.append(':');
        appendMinTwoDigits(sb, seconds);
        sb.append('.');
    }

    private static void appendMinTwoDigits(StringBuilder sb, long value) {
        if (value < 10) {
            sb.append('0');
        }
        sb.append(value);
    }

    private static void appendMinThreeDigits(StringBuilder sb, long value) {
        if (value < 100) {
            sb.append('0');
        }
        if (value < 10) {
            sb.append('0');
        }
        sb.append(value);
    }

    private static void appendMinSixDigits(StringBuilder sb, long value) {
        if (value < 100000) {
            sb.append('0');
        }
        if (value < 10000) {
            sb.append('0');
        }
        if (value < 1000) {
            sb.append('0');
        }
        if (value < 100) {
            sb.append('0');
        }
        if (value < 10) {
            sb.append('0');
        }
        sb.append(value);
    }

    private FormattedDuration() {}
}
