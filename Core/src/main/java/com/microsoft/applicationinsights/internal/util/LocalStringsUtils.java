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


    private static Calendar s_calendar;
    private static DateFormat s_dateFormatter;
    private static DateFormat s_durationFormatter;
}
