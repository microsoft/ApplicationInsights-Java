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

import static com.microsoft.applicationinsights.agent.internal.common.TelemetryTruncation.truncatePropertyValue;

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import java.util.Iterator;
import java.util.Map;

public class SanitizationHelper {
  public static final int MAX_KEY_LENGTH = 150;
  public static final int MAX_VALUE_LENGTH = 8192;
  public static final int MAX_NAME_LENGTH = 1024;
  public static final int MAX_ID_LENGTH = 512;
  public static final int MAX_MESSAGE_LENGTH = 32768;
  public static final int MAX_URL_LENGTH = 2048;

  /**
   * Function to sanitize both key and value in properties, see rules at
   * https://github.com/microsoft/common-schema/blob/main/Mappings/AzureMonitor-AI.md#mapping-rule
   */
  public static void sanitizeProperties(Map<String, String> properties) {
    if (properties == null) {
      return;
    }
    for (Iterator<Map.Entry<String, String>> i = properties.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<String, String> entry = i.next();
      String key = entry.getKey();
      String value = entry.getValue();
      if (Strings.isNullOrEmpty(key) || key.length() > MAX_KEY_LENGTH || value == null) {
        i.remove();
        continue;
      }
      if (value.length() > MAX_VALUE_LENGTH) {
        entry.setValue(truncatePropertyValue(value, MAX_VALUE_LENGTH, key));
      }
    }
  }

  /**
   * Function to sanitize both key and value in measurements, see rules at
   * https://github.com/microsoft/common-schema/blob/main/Mappings/AzureMonitor-AI.md#mapping-rule
   */
  public static void sanitizeMeasurements(Map<String, Double> measurements) {
    if (measurements == null) {
      return;
    }
    for (Iterator<Map.Entry<String, Double>> i = measurements.entrySet().iterator();
        i.hasNext(); ) {
      Map.Entry<String, Double> entry = i.next();
      String key = entry.getKey();
      if (Strings.isNullOrEmpty(key) || key.length() > MAX_KEY_LENGTH) {
        i.remove();
      }
    }
  }

  private SanitizationHelper() {}
}
