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

package com.microsoft.applicationinsights.telemetry;

/**
 * This class lets its users to define an interval of time
 * which can be defined in terms of days, hours, minutes, seconds and milliseconds.
 *
 * It has various constructors to let the user easily define an interval of time.
 *
 * Created by gupele on 2/8/2015.
 */
public final class Duration {
    private final static String DAYS_FORMAT = "%02d.";
    private final static String HH_MM_SS_FORMAT = "%02d:%02d:%02d";
    private final static String MILLISECONDS_FORMAT = ".%03d0000";

    private final int days;
    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;

    /**
     * The interval is set by setting all the possible values.
     * @param days Day(s) of interval (up to 99).
     * @param hours Hour(s) of interval (up to 99).
     * @param minutes Minute(s) of interval (up to 99).
     * @param seconds Second(s) of interval (up to 99).
     * @param milliseconds Milliseconds (up to 999).
     */
    public Duration(int days, int hours, int minutes, int seconds, int milliseconds) {
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
    }

    /**
     * The interval is set by setting all the possible values except the milliseconds.
     * @param days Day(s) of interval (up to 99).
     * @param hours Hour(s) of interval (up to 99).
     * @param minutes Minute(s) of interval (up to 99).
     * @param seconds Second(s) of interval (up to 99).
     */
    public Duration(int days, int hours, int minutes, int seconds) {
        this(days, hours, minutes, seconds, 0);
    }

    /**
     * The interval is defined by hours, minutes and seconds.
     * @param hours Hour(s) of interval (up to 99).
     * @param minutes Minute(s) of interval (up to 99).
     * @param seconds Second(s) of interval (up to 99).
     */
    public Duration(int hours, int minutes, int seconds) {
        this(0, hours, minutes, seconds, 0);

    }

    /**
     * The duration is defined by milliseconds.
     * The class will calculate the number of days, hours, minutes, seconds and milliseconds from that value.
     * @param duration The duration in milliseconds.
     */
    public Duration(long duration) {
        int div1000 = (int) (duration / 1000);
        milliseconds = (int) (duration % 1000);

        seconds = div1000 % 60;
        minutes = (div1000 / 60) % 60;
        hours = (div1000 / 3600) % 24;
        days = div1000 / 86400;
    }

    public int getDays() {
        return days;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMilliseconds() {
        return milliseconds;
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
        hash = 89 * hash + this.days;
        hash = 89 * hash + this.hours;
        hash = 89 * hash + this.minutes;
        hash = 89 * hash + this.seconds;
        hash = 89 * hash + this.milliseconds;
        return hash;
    }
}


