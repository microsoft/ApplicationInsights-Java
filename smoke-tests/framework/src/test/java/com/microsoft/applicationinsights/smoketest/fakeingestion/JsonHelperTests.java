// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Duration;
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
