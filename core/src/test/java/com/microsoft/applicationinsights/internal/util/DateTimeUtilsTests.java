package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

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
    void testParseRoundTripDateStringFullUTC() throws ParseException {
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

    private String parseRoundTripDateString(String str) throws ParseException {
        Date date = DateTimeUtils.parseRoundTripDateString(str);

        return DateTimeUtils.formatAsRoundTripDate(date);
    }
}