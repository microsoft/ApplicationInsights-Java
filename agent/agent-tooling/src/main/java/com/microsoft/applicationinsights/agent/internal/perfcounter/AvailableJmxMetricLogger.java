// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import io.opentelemetry.instrumentation.api.internal.GuardedBy;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO (trask) add tests
class AvailableJmxMetricLogger {

  private static final Logger logger = LoggerFactory.getLogger(AvailableJmxMetricLogger.class);

  private static final String NEWLINE = System.getProperty("line.separator");

  @GuardedBy("lock")
  private Map<String, Set<String>> priorAttributeMap = new HashMap<>();

  private final Object lock = new Object();

  void logAvailableJmxMetrics() {
    synchronized (lock) {
      Map<String, Set<String>> attributeMap = getAttributeMap();
      logDifference(priorAttributeMap, attributeMap);
      priorAttributeMap = attributeMap;
    }
  }

  private static void logDifference(
      Map<String, Set<String>> priorAvailableJmxAttributes,
      Map<String, Set<String>> currentAvailableJmxAttributes) {
    if (priorAvailableJmxAttributes.isEmpty()) {
      // first time
      logger.info("available jmx metrics:{}{}", NEWLINE, toString(currentAvailableJmxAttributes));
      return;
    }
    Map<String, Set<String>> newlyAvailable =
        difference(currentAvailableJmxAttributes, priorAvailableJmxAttributes);
    if (!newlyAvailable.isEmpty()) {
      logger.info(
          "newly available jmx metrics since last output:{}{}", NEWLINE, toString(newlyAvailable));
    }
    Map<String, Set<String>> noLongerAvailable =
        difference(priorAvailableJmxAttributes, currentAvailableJmxAttributes);
    if (!noLongerAvailable.isEmpty()) {
      logger.info(
          "no longer available jmx metrics since last output:{}{}",
          NEWLINE,
          toString(noLongerAvailable));
    }
  }

  private static String toString(Map<String, Set<String>> jmxAttributes) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Set<String>> entry : jmxAttributes.entrySet()) {
      sb.append("  - object name:        ")
          .append(entry.getKey())
          .append(NEWLINE)
          .append("    attributes: ")
          .append(entry.getValue().stream().sorted().collect(Collectors.joining(", ")))
          .append(NEWLINE);
    }
    return sb.toString();
  }

  private static Map<String, Set<String>> getAttributeMap() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectName> objectNames = server.queryNames(null, null);
    Map<String, Set<String>> attributeMap = new HashMap<>();
    for (ObjectName objectName : objectNames) {
      String name = objectName.toString();
      Set<String> attributes;
      try {
        attributes = getAttributes(server, objectName);
        if (attributes.isEmpty()) {
          attributes.add("(no attributes found)");
        }
      } catch (Exception e) {
        // log exception at trace level since this is expected in several cases, e.g.
        // "java.lang.UnsupportedOperationException: CollectionUsage threshold is not supported"
        // and available jmx metrics are already only logged at debug
        logger.trace(e.getMessage(), e);
        attributes = singleton("(error getting attributes)");
      }
      attributeMap.put(name, attributes);
    }
    return attributeMap;
  }

  private static Set<String> getAttributes(MBeanServer server, ObjectName objectName)
      throws Exception {
    MBeanInfo mbeanInfo = server.getMBeanInfo(objectName);
    Set<String> attributes = new HashSet<>();
    for (MBeanAttributeInfo attribute : mbeanInfo.getAttributes()) {
      if (!attribute.isReadable()) {
        attributes.add(attribute.getName() + " (not readable)");
        continue;
      }
      try {
        Object value = server.getAttribute(objectName, attribute.getName());
        attributes.addAll(getAttributes(attribute, value));
      } catch (Exception e) {
        // log exception at trace level since this is expected in several cases, e.g.
        // "java.lang.UnsupportedOperationException: CollectionUsage threshold is not supported"
        // and available jmx metrics are already only logged at debug
        logger.trace(e.getMessage(), e);
        attributes.add(attribute.getName() + " (exception)");
      }
    }
    return attributes;
  }

  private static List<String> getAttributes(MBeanAttributeInfo attribute, @Nullable Object value) {

    String attributeType = attribute.getType();

    if (attributeType.equals(CompositeData.class.getName())) {
      Object openType = attribute.getDescriptor().getFieldValue("openType");
      CompositeType compositeType = null;
      if (openType instanceof CompositeType) {
        compositeType = (CompositeType) openType;
      } else if (openType == null && value instanceof CompositeDataSupport) {
        compositeType = ((CompositeDataSupport) value).getCompositeType();
      }
      if (compositeType != null) {
        return getCompositeTypeAttributes(attribute, value, compositeType);
      }
    }

    return singletonList(attribute.getName() + " (" + valueType(value) + ")");
  }

  private static List<String> getCompositeTypeAttributes(
      MBeanAttributeInfo attribute, @Nullable Object compositeData, CompositeType compositeType) {
    List<String> attributes = new ArrayList<>();
    for (String itemName : compositeType.keySet()) {
      String attributeName = attribute.getName() + "." + itemName;
      OpenType<?> itemType = compositeType.getType(itemName);
      if (itemType == null) {
        attributes.add(attributeName + " (null)");
        continue;
      }
      if (compositeData instanceof CompositeData) {
        Object value = ((CompositeData) compositeData).get(itemName);
        attributes.add(attributeName + " (" + valueType(value) + ")");
      } else if (compositeData == null) {
        attributes.add(attributeName + " (unexpected: null)");
      } else {
        attributes.add(attributeName + " (unexpected: " + compositeData.getClass().getName() + ")");
      }
    }
    return attributes;
  }

  private static String valueType(@Nullable Object value) {
    if (value instanceof Number) {
      return "number";
    }
    if (value instanceof Boolean) {
      return "boolean";
    }
    if (value instanceof String) {
      try {
        Double.parseDouble((String) value);
        return "number";
      } catch (NumberFormatException e) {
        return "string";
      }
    }
    return "other";
  }

  // visible for testing
  static Map<String, Set<String>> difference(
      Map<String, Set<String>> map1, Map<String, Set<String>> map2) {
    Map<String, Set<String>> difference = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : map1.entrySet()) {
      String key = entry.getKey();
      Set<String> diff = difference(entry.getValue(), map2.get(key));
      if (!diff.isEmpty()) {
        difference.put(entry.getKey(), diff);
      }
    }
    return difference;
  }

  private static Set<String> difference(Set<String> set1, @Nullable Set<String> set2) {
    if (set2 == null) {
      return set1;
    }
    HashSet<String> difference = new HashSet<>(set1);
    difference.removeAll(set2);
    return difference;
  }
}
