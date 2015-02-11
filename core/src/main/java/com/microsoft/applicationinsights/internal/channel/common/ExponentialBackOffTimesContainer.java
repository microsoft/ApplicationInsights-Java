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

package com.microsoft.applicationinsights.internal.channel.common;

/**
 * This class creates the back-off timeouts by starting with five seconds
 * and expanding the interval up to six minutes, every interval is followed
 * by a five seconds timeout to make sure not all threads are block for long timeouts.
 *
 * Created by gupele on 2/10/2015.
 */
public final class ExponentialBackOffTimesContainer implements BackOffTimesContainer {
    private static final long FIVE_SECONDS = 5;
    private static final long TEN_SECONDS = 10;
    private static final long FIFTEEN_SECONDS = 15;
    private static final long THIRTY_SECONDS = 30;
    private static final long ONE_MINUTES_IN_SECONDS = 60;
    private static final long TWO_MINUTES_IN_SECONDS = 120;
    private static final long FOUR_MINUTES_IN_SECONDS = 240;
    private static final long SIX_MINUTES_IN_SECONDS = 360;
    private static long[] s_exponentialBackOffInSeconds = new long[] {
            FIVE_SECONDS,
            TEN_SECONDS,
            FIVE_SECONDS,
            FIFTEEN_SECONDS,
            FIVE_SECONDS,
            THIRTY_SECONDS,
            FIVE_SECONDS,
            ONE_MINUTES_IN_SECONDS,
            FIVE_SECONDS,
            TWO_MINUTES_IN_SECONDS,
            FIVE_SECONDS,
            FOUR_MINUTES_IN_SECONDS,
            FIVE_SECONDS,
            SIX_MINUTES_IN_SECONDS,
            FIVE_SECONDS
    };

    @Override
    public long[] getBackOffTimeoutsInSeconds() {
        return s_exponentialBackOffInSeconds;
    }
}
