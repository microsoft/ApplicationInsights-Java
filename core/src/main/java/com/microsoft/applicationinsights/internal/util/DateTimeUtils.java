/*
 * ApplicationInsights-Java
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by yonisha on 2/5/2015.
 */
public class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static Date getDateTimeNow() {
        return new Date();
    }

    public static Date addToDate(Date date, int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);

        return calendar.getTime();
    }

    public static long getDateDiff(Date laterDate, Date date, TimeUnit timeUnit) {
        long diffInMs = laterDate.getTime() - date.getTime();

        return timeUnit.convert(diffInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Parses the given round-trip date string into Date object.
     * @param roundTripString The string to parse.
     * @return Date represents the string.
     * @throws java.text.ParseException Thrown when failed to parse the given string.
     */
    public static Date parseRoundTripDateString(String roundTripString) throws ParseException {
        SimpleDateFormat roundTripDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'");
        Date date = roundTripDateFormat.parse(roundTripString);

        return date;
    }

    /**
     * Formats the given date as round-trip date string
     * @param date Round-trip date string
     * @return Round-trip date string
     */
    public static String formatAsRoundTripDate(Date date) {
        SimpleDateFormat roundTripDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'");
        return roundTripDateFormat.format(date);
    }
}
