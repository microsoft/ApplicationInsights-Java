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

package com.microsoft.applicationinsights.telemetry;

import com.google.common.base.Preconditions;

/**
 * This class lets its users to define an interval of time
 * which can be defined in terms of days, hours, minutes, seconds and milliseconds.
 *
 * It has various constructors to let the user easily define an interval of time.
 */
public final class Duration {
    private final static String DAYS_FORMAT = "%02d.";
    private final static String HH_MM_SS_FORMAT = "%02d:%02d:%02d";
    private final static String MILLISECONDS_FORMAT = ".%03d0000";

    private final static long SECONDS_IN_ONE_MINUTE = 60;
    private final static long SECONDS_IN_ONE_HOUR = 3600;
    private final static long SECONDS_IN_ONE_DAY = 86400;
    private final static long MINUTES_IN_ONE_HOUR = 60;
    private final static long HOURS_IN_ONE_DAY = 24;

    private final long days;
    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;

    /**
     * The interval is set by setting all the possible values.
     * @param days Day(s).
     * @param hours Hour(s) in range [-23, 23].
     * @param minutes Minute(s) in range [-59, 59].
     * @param seconds Second(s) in range [-59, 59].
     * @param milliseconds Milliseconds in range [0, 999].
     */
    public Duration(long days, int hours, int minutes, int seconds, int milliseconds) {
        Preconditions.checkArgument(hours >= -23 && hours <= 23, "hours argument should be in the [-23, 23] range");
        Preconditions.checkArgument(minutes >= -59 && minutes <= 59, "minutes argument should be in the [-59, 59] range");
        Preconditions.checkArgument(seconds >= -59 && seconds <= 59, "seconds argument should be in the [-59, 59] range");
        Preconditions.checkArgument(milliseconds >= 0 && milliseconds <= 999, "milliseconds argument should be in the [0, 999] range");

        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
    }

    /**
     * The duration is defined by milliseconds.
     * The class will calculate the number of days, hours, minutes, seconds and milliseconds from that value.
     * @param duration The duration in milliseconds.
     */
    public Duration(long duration) {
        milliseconds = (int)(duration % 1000);

        long durationInSeconds = duration / 1000;
        seconds = (int)(durationInSeconds % SECONDS_IN_ONE_MINUTE);
        minutes = (int)((durationInSeconds / SECONDS_IN_ONE_MINUTE) % MINUTES_IN_ONE_HOUR);
        hours = (int)((durationInSeconds / SECONDS_IN_ONE_HOUR) % HOURS_IN_ONE_DAY);
        days = durationInSeconds / SECONDS_IN_ONE_DAY;
    }

    /**
     * Gets the days part of the duration.
     * @return The days part of the duration.
     */
    public long getDays() {
        return days;
    }

    /**
     * Gets the hours part of the duration.
     * @return The hours part of the duration.
     */
    public int getHours() {
        return hours;
    }

    /**
     * Gets the minutes part of the duration.
     * @return The minutes part of the duration.
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * Gets the seconds part of the duration.
     * @return The seconds part of the duration.
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Gets the milliseconds part of the duration.
     * @return The milliseconds part of the duration.
     */
    public int getMilliseconds() {
        return milliseconds;
    }

    /**
     * Gets the total milliseconds of the duration.
     * @return The total milliseconds of the duration.
     */
    public long getTotalMilliseconds() {
        return  (days * SECONDS_IN_ONE_DAY * 1000) +
                (hours * SECONDS_IN_ONE_HOUR * 1000) +
                (minutes * SECONDS_IN_ONE_MINUTE * 1000) +
                (seconds * 1000) + milliseconds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (days != 0) {
            sb.append(String.format(DAYS_FORMAT, days));
        }
        sb.append(String.format(HH_MM_SS_FORMAT, hours, minutes, seconds));
        if (milliseconds > 0) {
            sb.append(String.format(MILLISECONDS_FORMAT, milliseconds));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Duration)) {
            return false;
        }

        Duration that = (Duration)other;
        return
                this.days == that.getDays() &&
                this.hours == that.getHours() &&
                this.minutes == that.getMinutes() &&
                this.seconds == that.getSeconds() &&
                this.milliseconds == that.getMilliseconds();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (int)((days ^ (days >>> 32)));
        hash = 89 * hash + hours;
        hash = 89 * hash + minutes;
        hash = 89 * hash + seconds;
        hash = 89 * hash + milliseconds;
        return hash;
    }
}


