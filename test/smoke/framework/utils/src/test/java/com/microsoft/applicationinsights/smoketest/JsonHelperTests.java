package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.telemetry.Duration;
import org.junit.Assert;
import org.junit.Test;

public class JsonHelperTests {
  @Test
  public void testMillisecondDuration() {
    Duration expected = new Duration(1);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    expected = new Duration(0, 0, 0, 0, 1);
    json = getJsonDurationInHolder(expected);
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  @Test
  public void testDayDuration() {
    Duration expected = new Duration(2, 0, 0, 0, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    expected = new Duration(2L * 86400000L);
    json = getJsonDurationInHolder(expected);
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  @Test
  public void testSecondDuration() {
    Duration expected = new Duration(0, 0, 0, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    json = getJsonDurationInHolder(new Duration(50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  @Test
  public void testHourDuration() {
    Duration expected = new Duration(0, 1, 0, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    json = getJsonDurationInHolder(new Duration(3600000L + 50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  @Test
  public void testMinuteDuration() {
    Duration expected = new Duration(0, 0, 25, 50, 0);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    json = getJsonDurationInHolder(new Duration(25 * 60000L + 50000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  @Test
  public void testDaysAndMilliseconds() {
    Duration expected = new Duration(5, 0, 1, 0, 213);
    String json = getJsonDurationInHolder(expected);
    System.out.println(json);
    DurationHolder actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());

    json = getJsonDurationInHolder(new Duration(60213L + 5 * 86400000L));
    System.out.println(json);
    actual = JsonHelper.GSON.fromJson(json, DurationHolder.class);
    Assert.assertEquals(expected, actual.getData());
  }

  private String getJsonDurationInHolder(Duration d) {
    return "{\"data\":\"" + d.toString() + "\"}";
  }

  public class DurationHolder {
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
