package com.microsoft.applicationinsights.internal.util;

import java.text.ParseException;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

/** Created by yonisha on 5/26/2015. */
public class DateTimeUtilsTests {

  @Test
  public void testParseRoundTripDateString() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14+01:00";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    Assert.assertEquals("2015-05-26T07", reversedDateStr);
  }

  @Test
  public void testParseRoundTripDateStringWithoutOffset() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14.123145152";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    Assert.assertEquals("2015-05-26T07", reversedDateStr);
  }

  @Test
  public void testParseRoundTripDateStringFullUTC() throws ParseException {
    final String dateStr = "2015-05-26T07:00:14.123145152Z";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    Assert.assertEquals("2015-05-26T07", reversedDateStr);
  }

  @Test
  public void testParseRoundTripDateStringShortFormat() throws ParseException {
    final String dateStr = "2015-05-26T07";
    String reversedDateStr = parseRoundTripDateString(dateStr);

    Assert.assertEquals(dateStr, reversedDateStr);
  }

  @Test
  public void testFormatAsRoundTripDate() throws ParseException {
    final String dateStr = "2016-01-21T01";
    Date date = DateTimeUtils.parseRoundTripDateString(dateStr);
    String reversedDateStr = DateTimeUtils.formatAsRoundTripDate(date);

    Assert.assertEquals(dateStr, reversedDateStr);
  }

  private String parseRoundTripDateString(String str) throws ParseException {
    Date date = DateTimeUtils.parseRoundTripDateString(str);

    return DateTimeUtils.formatAsRoundTripDate(date);
  }
}
