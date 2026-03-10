// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MessageData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryExceptionData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;

final class GenAiPropertyUtil {

  static final Set<String> GEN_AI_ATTRIBUTE_KEYS =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "gen_ai.input.messages",
                  "gen_ai.output.messages",
                  "gen_ai.system_instructions",
                  "gen_ai.tool.definitions",
                  "gen_ai.tool.call.arguments",
                  "gen_ai.tool.call.result",
                  "gen_ai.evaluation.explanation")));

  static Map<String, String> extractGenAiAttributes(Attributes attributes) {
    Map<String, String> genAiValues = new HashMap<>();
    attributes.forEach(
        (key, value) -> {
          if (GEN_AI_ATTRIBUTE_KEYS.contains(key.getKey())) {
            String stringValue = convertToString(value, key.getType());
            if (stringValue != null) {
              genAiValues.put(key.getKey(), stringValue);
            }
          }
        });
    return genAiValues;
  }

  static void restoreGenAiProperties(TelemetryItem telemetryItem, Map<String, String> genAiValues) {
    Map<String, String> properties = getProperties(telemetryItem);
    if (properties != null) {
      properties.putAll(genAiValues);
    }
  }

  @Nullable
  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  private static String convertToString(Object value, AttributeType type) {
    switch (type) {
      case STRING:
      case BOOLEAN:
      case LONG:
      case DOUBLE:
        return String.valueOf(value);
      case STRING_ARRAY:
      case BOOLEAN_ARRAY:
      case LONG_ARRAY:
      case DOUBLE_ARRAY:
        return joinList((List<?>) value);
      default:
        return null;
    }
  }

  private static <T> String joinList(List<T> list) {
    StringBuilder sb = new StringBuilder();
    for (T item : list) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(item);
    }
    return sb.toString();
  }

  @Nullable
  private static Map<String, String> getProperties(TelemetryItem telemetryItem) {
    if (telemetryItem.getData() == null || telemetryItem.getData().getBaseData() == null) {
      return null;
    }
    MonitorDomain baseData = telemetryItem.getData().getBaseData();
    if (baseData instanceof MessageData) {
      MessageData data = (MessageData) baseData;
      return getOrInitProperties(data::getProperties, data::setProperties);
    } else if (baseData instanceof RequestData) {
      RequestData data = (RequestData) baseData;
      return getOrInitProperties(data::getProperties, data::setProperties);
    } else if (baseData instanceof RemoteDependencyData) {
      RemoteDependencyData data = (RemoteDependencyData) baseData;
      return getOrInitProperties(data::getProperties, data::setProperties);
    } else if (baseData instanceof TelemetryExceptionData) {
      TelemetryExceptionData data = (TelemetryExceptionData) baseData;
      return getOrInitProperties(data::getProperties, data::setProperties);
    } else if (baseData instanceof TelemetryEventData) {
      TelemetryEventData data = (TelemetryEventData) baseData;
      return getOrInitProperties(data::getProperties, data::setProperties);
    }
    return null;
  }

  private static Map<String, String> getOrInitProperties(
      Supplier<Map<String, String>> getter, Consumer<Map<String, String>> setter) {
    Map<String, String> properties = getter.get();
    if (properties == null) {
      properties = new HashMap<>();
      setter.accept(properties);
    }
    return properties;
  }

  private GenAiPropertyUtil() {}
}
