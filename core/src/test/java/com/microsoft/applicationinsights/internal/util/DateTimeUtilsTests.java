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

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTests {

  @Test
  void testParseRoundTripDateString() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14+01:00";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    assertThat(reversedDateStr).isEqualTo("2015-05-26T07");
  }

  @Test
  void testParseRoundTripDateStringWithoutOffset() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14.123145152";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    assertThat(reversedDateStr).isEqualTo("2015-05-26T07");
  }

  @Test
  void testParseRoundTripDateStringFullUtc() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14.123145152Z";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    assertThat(reversedDateStr).isEqualTo("2015-05-26T07");
  }

  @Test
  void testParseRoundTripDateStringShortFormat() throws ParseException {
    final String dateStr = "2015-05-26T07";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    assertThat(reversedDateStr).isEqualTo(dateStr);
  }

  @Test
  void testFormatAsRoundTripDate() throws ParseException {
    final String dateStr = "2016-01-21T01";
    Date date = DateTimeUtils.parseRoundTripDateString(dateStr);
    String reversedDateStr = DateTimeUtils.formatAsRoundTripDate(date);

    assertThat(reversedDateStr).isEqualTo(dateStr);
  }

  private static String parseRoundTripDateString(String str) throws ParseException {
    Date date = DateTimeUtils.parseRoundTripDateString(str);

    return DateTimeUtils.formatAsRoundTripDate(date);
  }
}
