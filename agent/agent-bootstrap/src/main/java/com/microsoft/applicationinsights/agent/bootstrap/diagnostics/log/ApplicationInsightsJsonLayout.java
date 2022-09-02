// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsValueFinder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplicationInsightsJsonLayout extends JsonLayout {

  public static final String TIMESTAMP_PROP_NAME = "time";
  public static final String OPERATION_NAME_PROP_NAME = "operation";
  public static final String CUSTOM_FIELDS_PROP_NAME = "properties";

  // visible for testing
  static final String UNKNOWN_VALUE = "unknown";

  // visible for testing
  final List<DiagnosticsValueFinder> valueFinders = new ArrayList<>();

  public ApplicationInsightsJsonLayout() {
    ApplicationMetadataFactory mf = DiagnosticsHelper.getMetadataFactory();
    valueFinders.add(mf.getSiteName());
    valueFinders.add(mf.getInstrumentationKey());
    valueFinders.add(mf.getExtensionVersion());
    valueFinders.add(mf.getSdkVersion());
    valueFinders.add(mf.getSubscriptionId());
  }

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, Object> jsonMap = new LinkedHashMap<>();
    addTimestamp(TIMESTAMP_PROP_NAME, true, event.getTimeStamp(), jsonMap);
    add(LEVEL_ATTR_NAME, true, String.valueOf(event.getLevel()), jsonMap);
    add(LOGGER_ATTR_NAME, true, event.getLoggerName(), jsonMap);
    add(FORMATTED_MESSAGE_ATTR_NAME, true, event.getFormattedMessage(), jsonMap);
    addThrowableInfo(EXCEPTION_ATTR_NAME, true, event, jsonMap);
    addMap(CUSTOM_FIELDS_PROP_NAME, true, getPropertiesMap(event), jsonMap);
    return jsonMap;
  }

  private Map<String, Object> getPropertiesMap(ILoggingEvent event) {
    Map<String, Object> jsonMap = new LinkedHashMap<>();
    add(OPERATION_NAME_PROP_NAME, true, getOperationName(event), jsonMap);
    add(DiagnosticsHelper.MDC_MESSAGE_ID, true, getMessageId(event), jsonMap);
    for (DiagnosticsValueFinder finder : valueFinders) {
      String value = finder.getValue();
      add(
          finder.getName(),
          true,
          value == null || value.isEmpty() ? UNKNOWN_VALUE : value,
          jsonMap);
    }
    jsonMap.put("language", "java");
    return jsonMap;
  }

  private static String getOperationName(ILoggingEvent event) {
    return event.getMDCPropertyMap().get(DiagnosticsHelper.MDC_PROP_OPERATION);
  }

  private static String getMessageId(ILoggingEvent event) {
    return event.getMDCPropertyMap().get(DiagnosticsHelper.MDC_MESSAGE_ID);
  }
}
