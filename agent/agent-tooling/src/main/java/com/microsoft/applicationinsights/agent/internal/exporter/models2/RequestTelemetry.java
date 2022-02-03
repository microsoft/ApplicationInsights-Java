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

package com.microsoft.applicationinsights.agent.internal.exporter.models2;

import static com.microsoft.applicationinsights.agent.internal.common.TelemetryTruncation.truncateTelemetry;

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RequestData;
import com.microsoft.applicationinsights.agent.internal.exporter.utils.SanitizationHelper;
import java.util.HashMap;
import java.util.Map;

public final class RequestTelemetry extends Telemetry {

  private static final int MAX_SOURCE_LENGTH = 1024;
  private static final int MAX_RESPONSE_CODE_LENGTH = 1024;

  private final RequestData data;

  public static RequestTelemetry create(String instrumentationKey, Map<String, String> globalTags) {
    return new RequestTelemetry(instrumentationKey, new RequestData(), globalTags);
  }

  private RequestTelemetry(
      String instrumentationKey, RequestData data, Map<String, String> globalTags) {
    super(data, "Request", "RequestData", instrumentationKey, globalTags);
    this.data = data;
  }

  public void setId(String id) {
    data.setId(truncateTelemetry(id, SanitizationHelper.MAX_ID_LENGTH, "RequestData.id"));
  }

  public void setName(String name) {
    data.setName(truncateTelemetry(name, SanitizationHelper.MAX_NAME_LENGTH, "RequestData.name"));
  }

  public void setDuration(String duration) {
    data.setDuration(duration);
  }

  public void setSuccess(boolean success) {
    data.setSuccess(success);
  }

  public void setResponseCode(String responseCode) {
    data.setResponseCode(
        truncateTelemetry(responseCode, MAX_RESPONSE_CODE_LENGTH, "RequestData.responseCode"));
  }

  public void setSource(String source) {
    data.setSource(truncateTelemetry(source, MAX_SOURCE_LENGTH, "RequestData.source"));
  }

  public void setUrl(String url) {
    data.setUrl(truncateTelemetry(url, SanitizationHelper.MAX_URL_LENGTH, "RequestData.url"));
  }

  public void addMeasurement(String key, Double value) {
    if (Strings.isNullOrEmpty(key) || key.length() > MAX_MEASUREMENT_KEY_LENGTH) {
      // TODO (trask) log
      return;
    }
    Map<String, Double> measurements = data.getMeasurements();
    if (measurements == null) {
      measurements = new HashMap<>();
      data.setMeasurements(measurements);
    }
    measurements.put(key, value);
  }

  @Override
  protected Map<String, String> getProperties() {
    Map<String, String> properties = data.getProperties();
    if (properties == null) {
      properties = new HashMap<>();
      data.setProperties(properties);
    }
    return properties;
  }
}
