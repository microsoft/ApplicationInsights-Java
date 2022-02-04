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

package com.microsoft.applicationinsights.agent.internal.exporter.models.builders;

import static com.microsoft.applicationinsights.agent.internal.common.TelemetryTruncation.truncatePropertyValue;

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorBase;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTelemetryBuilder {

  private static final int MAX_PROPERTY_KEY_LENGTH = 150;
  private static final int MAX_PROPERTY_VALUE_LENGTH = 8192;

  protected static final int MAX_MEASUREMENT_KEY_LENGTH = 150;

  private final TelemetryItem telemetryItem;

  protected AbstractTelemetryBuilder(MonitorDomain data, String telemetryName, String baseType) {

    telemetryItem = new TelemetryItem();
    telemetryItem.setVersion(1);
    telemetryItem.setName(telemetryName);

    data.setVersion(2);

    MonitorBase monitorBase = new MonitorBase();
    telemetryItem.setData(monitorBase);
    monitorBase.setBaseType(baseType);
    monitorBase.setBaseData(data);
  }

  public void setTime(OffsetDateTime time) {
    telemetryItem.setTime(time);
  }

  public void setSampleRate(float sampleRate) {
    telemetryItem.setSampleRate(sampleRate);
  }

  public void setInstrumentationKey(String instrumentationKey) {
    telemetryItem.setInstrumentationKey(instrumentationKey);
  }

  public void addTag(String key, String value) {
    Map<String, String> tags = telemetryItem.getTags();
    if (tags == null) {
      tags = new HashMap<>();
      telemetryItem.setTags(tags);
    }
    tags.put(key, value);
  }

  public void addProperty(String key, String value) {
    if (Strings.isNullOrEmpty(key) || key.length() > MAX_PROPERTY_KEY_LENGTH || value == null) {
      // TODO (trask) log
      return;
    }
    getProperties().put(key, truncatePropertyValue(value, MAX_PROPERTY_VALUE_LENGTH, key));
  }

  public TelemetryItem build() {
    return telemetryItem;
  }

  protected abstract Map<String, String> getProperties();
}
