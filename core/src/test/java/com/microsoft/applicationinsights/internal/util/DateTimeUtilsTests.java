package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by yonisha on 5/26/2015.
 */
public class DateTimeUtilsTests {

    @Test
    public void testParseRoundTripDateString() throws ParseException {
        final String dateStr = "2015-05-26T07:00:14+01:00";
        String reversedDateStr = parseRoundTripDateString(dateStr);

        assertEquals("2015-05-26T07", reversedDateStr);
    }

    @Test
    public void testParseRoundTripDateStringWithoutOffset() throws ParseException {
        final String dateStr = "2015-05-26T07:00:14.123145152";
        String reversedDateStr = parseRoundTripDateString(dateStr);

        assertEquals("2015-05-26T07", reversedDateStr);
    }

    @Test
    public void testParseRoundTripDateStringFullUTC() throws ParseException {
        final String dateStr = "2015-05-26T07:00:14.123145152Z";
        String reversedDateStr = parseRoundTripDateString(dateStr);

        assertEquals("2015-05-26T07", reversedDateStr);
    }

    @Test
    public void testParseRoundTripDateStringShortFormat() throws ParseException {
        final String dateStr = "2015-05-26T07";
        String reversedDateStr = parseRoundTripDateString(dateStr);

        assertEquals(dateStr, reversedDateStr);
    }

    @Test
    public void testFormatAsRoundTripDate() throws ParseException {
        final String dateStr = "2016-01-21T01";
        Date date = DateTimeUtils.parseRoundTripDateString(dateStr);
        String reversedDateStr = DateTimeUtils.formatAsRoundTripDate(date);

        assertEquals(dateStr, reversedDateStr);
    }

    private String parseRoundTripDateString(String str) throws ParseException {
        Date date = DateTimeUtils.parseRoundTripDateString(str);

        return DateTimeUtils.formatAsRoundTripDate(date);
    }
}