package com.microsoft.applicationinsights.internal.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class LocalStringsUtils
{
    /**
     * Determine whether a string is null or empty.
     * @param value The string value
     * @return True if the string is either null or empty.
     */
    public static boolean isNullOrEmpty(String value)
    {
        return value == null || value.isEmpty();
    }

    public static String populateRequiredStringWithNullValue(String value, String parameterName, String telemetryType)
    {
        if (isNullOrEmpty(value))
        {
            return parameterName + " is a required field for " + telemetryType;
        }

        return value;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean tryParseByte(String str)
    {
        try
        {
            Byte.parseByte(str);
            return true;
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }
    }

    public static String sanitize(String str, int maxLength)
    {
        if (!isNullOrEmpty(str))
        {
            str = str.trim();

            if (str.length() > maxLength)
                str = str.substring(0, maxLength);
        }

        return str;
    }

    public static String formatDuration(long milliSeconds)
    {
        // The trick here is to treat the duration as a point in time, and then format
        // only the time portion of it. There is no JDK-supported formatting of durations
        // until JDK 8, which we don't want to take a dependency on for something this
        // trivial.

        Calendar calendar = getCalendar();

        int offset = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));

        // Compensate for time zone offset and avoid a negative time.
        milliSeconds = milliSeconds + offset + 24*3600*1000;

        return getDurationFormatter().format(new Date(milliSeconds));
    }

    public static String generateRandomId()
    {
        return UUID.randomUUID().toString();
    }

    public static int generateRandomIntegerId()
    {
        long lsb = UUID.randomUUID().getLeastSignificantBits();
        return (int)(lsb & 0x7FFFFFFF);
    }

    public static DateFormat getDateFormatter()
    {
        if (s_dateFormatter == null)
            s_dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return s_dateFormatter;
    }


    private static DateFormat getDurationFormatter()
    {
        if (s_durationFormatter == null)
            s_durationFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
        return s_durationFormatter;
    }

    private static Calendar getCalendar()
    {
        if (s_calendar == null)
        {
            s_calendar = GregorianCalendar.getInstance();
        }
        return s_calendar;
    }

    private static Calendar s_calendar;
    private static DateFormat s_dateFormatter;
    private static DateFormat s_durationFormatter;
}
