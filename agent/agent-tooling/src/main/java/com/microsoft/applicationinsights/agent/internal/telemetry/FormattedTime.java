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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.microsoft.applicationinsights.agent.internal.exporter.models.TimeData;
import java.util.Date;

public class FormattedTime {

  public static TimeData offSetDateTimeFromNow() {
    return offSetDateTimeFromEpochMillis(System.currentTimeMillis());
  }

  public static TimeData offSetDateTimeFromDate(Date date) {
    return offSetDateTimeFromEpochMillis(date.getTime());
  }

  public static TimeData offSetDateTimeFromEpochNanos(long epochNanos) {
    return offSetDateTimeFromEpochMillis(NANOSECONDS.toMillis(epochNanos));
  }

  public static TimeData offSetDateTimeFromEpochMillis(long epochMillis) {
    return new TimeData(epochMillis);
  }

  private FormattedTime() {}
}
