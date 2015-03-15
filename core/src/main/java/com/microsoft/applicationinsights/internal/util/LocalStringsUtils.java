/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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

    public static String generateRandomId(boolean removeDashes)
    {
        String uuid = UUID.randomUUID().toString();

        if (removeDashes) {
            uuid = uuid.replace("-", "");
        }

        return uuid;
    }

    public static String generateRandomIntegerId()
    {
        Random random = new Random();
        long abs = Math.abs(random.nextLong());

        return String.valueOf(abs);
    }

    public static DateFormat getDateFormatter()
    {
        if (s_dateFormatter == null)
            s_dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        return s_dateFormatter;
    }


    private static Calendar s_calendar;
    private static DateFormat s_dateFormatter;
    private static DateFormat s_durationFormatter;
}
