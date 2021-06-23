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

package com.microsoft.applicationinsights.serviceprofilerapi.client.contract;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code TimestampContract} class is used to convert between Java profiler timestamps and Java
 * standard time types {@link OffsetDateTime} and {@link Date}.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public final class TimestampContract {
  // Cant use ISO_INSTANT as it does not pad the nanos to 7 figures
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nX");

  public static String timestampToString(OffsetDateTime timestamp) {
    // This will give you an ISO 8601 compliant format and the use of UTC will
    // ensure that there are no issues with time zones etc.
    ZonedDateTime utc = timestamp.atZoneSameInstant(ZoneOffset.UTC);
    return utc.format(FORMATTER);
  }

  public static String timestampToString(long timestamp) {
    return timestampToString(timestampToOffsetDateTime(timestamp));
  }

  public static String timestampToString(Date timestamp) {
    return timestampToString(dateToOffsetDateTime(timestamp));
  }

  public static OffsetDateTime timestampToOffsetDateTime(long timestamp) {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
  }

  private static OffsetDateTime dateToOffsetDateTime(Date timestamp) {
    return timestamp.toInstant().atOffset(ZoneOffset.UTC);
  }

  public static String padNanos(String timestamp) {
    // ensure that timestamp has 7 nano figures
    Matcher matcher = Pattern.compile(".*\\.([0-9]+)Z$").matcher(timestamp);
    if (matcher.matches()) {
      int currentFigures = matcher.group(1).length();
      int pad = 7 - currentFigures;
      StringBuilder buffer = new StringBuilder(timestamp);
      for (int i = 0; i < pad; i++) {
        buffer.insert(buffer.length() - 1, "0");
      }
      timestamp = buffer.toString();
    }
    return timestamp;
  }

  private TimestampContract() {}
}
