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

package com.microsoft.applicationinsights.agent.internal.exporter.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class SanitizationHelperTests {

  @Test
  public void testValidPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    properties.put("key1", "value1");
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties).containsEntry("key1", "value1");
    assertThat(properties).containsEntry("key2", "value2");
  }

  @Test
  public void testValidMeasurementsData() {
    Map<String, Double> measurements = new HashMap<>();
    measurements.put("key1", 1.0);
    measurements.put("key2", 2.0);
    SanitizationHelper.sanitizeMeasurements(measurements);
    assertThat(measurements).containsEntry("key1", 1.0);
    assertThat(measurements).containsEntry("key2", 2.0);
  }

  @Test
  public void testEmptyKeyInPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    properties.put("", "value1");
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties).doesNotContainKey("");
    assertThat(properties).containsEntry("empty", "value1");
    assertThat(properties).containsEntry("key2", "value2");
  }

  @Test
  public void testEmptyKeyInMeasurementsData() {
    Map<String, Double> measurements = new HashMap<>();
    measurements.put("", 1.0);
    measurements.put("key2", 2.0);
    SanitizationHelper.sanitizeMeasurements(measurements);
    assertThat(measurements).doesNotContainKey("");
    assertThat(measurements).containsEntry("empty", 1.0);
    assertThat(measurements).containsEntry("key2", 2.0);
  }

  @Test
  public void testEmptyValueInPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    properties.put("key1", "");
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties).doesNotContainKey("key1");
    assertThat(properties).containsEntry("key2", "value2");
  }

  @Test
  public void testVeryLongKeyInPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < SanitizationHelper.MAX_KEY_NAME_LENGTH + 1; i++) {
      sb.append('a');
    }
    String longKey = sb.toString();
    properties.put(longKey, "value1");
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties).doesNotContainKey(longKey);
    assertThat(properties)
        .containsEntry(longKey.substring(0, SanitizationHelper.MAX_KEY_NAME_LENGTH), "value1");
    assertThat(properties).containsEntry("key2", "value2");
  }

  @Test
  public void testVeryLongKeyInMeasurementsData() {
    Map<String, Double> measurements = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < SanitizationHelper.MAX_KEY_NAME_LENGTH + 1; i++) {
      sb.append('a');
    }
    String longKey = sb.toString();
    measurements.put(longKey, 1.0);
    measurements.put("key2", 2.0);
    SanitizationHelper.sanitizeMeasurements(measurements);
    assertThat(measurements).doesNotContainKey(longKey);
    assertThat(measurements)
        .containsEntry(longKey.substring(0, SanitizationHelper.MAX_KEY_NAME_LENGTH), 1.0);
    assertThat(measurements).containsEntry("key2", 2.0);
  }

  @Test
  public void testVeryLongValueInPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < SanitizationHelper.MAX_VALUE_LENGTH + 1; i++) {
      sb.append('a');
    }
    String longValue = sb.toString();
    properties.put("key1", longValue);
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties.get("key1").length()).isEqualTo(SanitizationHelper.MAX_VALUE_LENGTH);
    assertThat(properties).containsEntry("key2", "value2");
  }

  @Test
  public void testVeryLongDuplicateKeyInPropertiesData() {
    Map<String, String> properties = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < SanitizationHelper.MAX_KEY_NAME_LENGTH + 1; i++) {
      sb.append('a');
    }
    String longKey1 = sb.append("key1").toString();
    String longKey2 = sb.append("key2").toString();
    properties.put(longKey1, "value1");
    properties.put(longKey2, "value1");
    properties.put("key2", "value2");
    SanitizationHelper.sanitizeProperties(properties);
    assertThat(properties).doesNotContainKey(longKey1);
    assertThat(properties).doesNotContainKey(longKey2);
    assertThat(properties)
        .containsEntry(longKey1.substring(0, SanitizationHelper.MAX_KEY_NAME_LENGTH), "value1");
    assertThat(properties)
        .containsEntry(
            longKey2.substring(0, SanitizationHelper.MAX_KEY_NAME_LENGTH - 3) + "1", "value1");
    assertThat(properties).containsEntry("key2", "value2");
  }
}
