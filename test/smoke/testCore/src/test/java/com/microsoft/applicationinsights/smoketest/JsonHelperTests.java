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

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.telemetry.Duration;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SystemOut")
public class JsonHelperTests {

  @Test
  public void testMillisecondDuration() {
    Duration expected = new Duration(1);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    expected = new Duration(0, 0, 0, 0, 1);
    json = getJsonDurationInHolder(expected);
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  @Test
  public void testDayDuration() {
    Duration expected = new Duration(2, 0, 0, 0, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    expected = new Duration(2L * 86400000L);
    json = getJsonDurationInHolder(expected);
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  @Test
  public void testSecondDuration() {
    Duration expected = new Duration(0, 0, 0, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    json = getJsonDurationInHolder(new Duration(50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  @Test
  public void testHourDuration() {
    Duration expected = new Duration(0, 1, 0, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    json = getJsonDurationInHolder(new Duration(3600000L + 50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  @Test
  public void testMinuteDuration() {
    Duration expected = new Duration(0, 0, 25, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    json = getJsonDurationInHolder(new Duration(25 * 60000L + 50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  @Test
  public void testDaysAndMilliseconds() {
    Duration expected = new Duration(5, 0, 1, 0, 213);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);

    json = getJsonDurationInHolder(new Duration(60213L + 5 * 86400000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    assertThat(actual.getData()).isEqualTo(expected);
  }

  private static String getJsonDurationInHolder(Duration d) {
    return "{\"data\":\"" + d.toString() + "\"}";
  }

  public static class DurationHolder {
    private Duration data;

    public DurationHolder(Duration data) {
      this.data = data;
    }

    public Duration getData() {
      return data;
    }

    public void setData(Duration data) {
      this.data = data;
    }
  }
}
