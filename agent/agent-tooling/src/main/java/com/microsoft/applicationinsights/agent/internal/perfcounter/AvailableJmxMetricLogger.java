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

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
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
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AvailableJmxMetricLogger {

  private static final Logger logger = LoggerFactory.getLogger(AvailableJmxMetricLogger.class);

  private static final Set<String> NUMERIC_ATTRIBUTE_TYPES =
      new HashSet<>(
          asList(
              "long",
              "int",
              "double",
              "float",
              "java.lang.Long",
              "java.lang.Integer",
              "java.lang.Double",
              "java.lang.Float"));

  private static final Set<String> BOOLEAN_ATTRIBUTE_TYPES =
      new HashSet<>(asList("boolean", "java.lang.Boolean"));

  @GuardedBy("lock")
  private Map<String, Set<String>> priorAvailableJmxAttributes = new HashMap<>();

  private final Object lock = new Object();

  void logAvailableJmxMetrics() {
    synchronized (lock) {
      Map<String, Set<String>> availableJmxAttributes = getAvailableJmxAttributes();
      logDifference(priorAvailableJmxAttributes, availableJmxAttributes);
      priorAvailableJmxAttributes = availableJmxAttributes;
    }
  }

  private static void logDifference(
      Map<String, Set<String>> priorAvailableJmxAttributes,
      Map<String, Set<String>> currentAvailableJmxAttributes) {
    if (priorAvailableJmxAttributes.isEmpty()) {
      // first time
      logger.info("available jmx metrics:\n{}", toString(currentAvailableJmxAttributes));
      return;
    }
    Map<String, Set<String>> newlyAvailable =
        difference(currentAvailableJmxAttributes, priorAvailableJmxAttributes);
    if (!newlyAvailable.isEmpty()) {
      logger.info("newly available jmx metrics since last output:\n{}", toString(newlyAvailable));
    }
    Map<String, Set<String>> noLongerAvailable =
        difference(priorAvailableJmxAttributes, currentAvailableJmxAttributes);
    if (!noLongerAvailable.isEmpty()) {
      logger.info(
          "no longer available jmx metrics since last output:\n{}", toString(noLongerAvailable));
    }
  }

  private static String toString(Map<String, Set<String>> jmxAttributes) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Set<String>> entry : jmxAttributes.entrySet()) {
      sb.append("  - object name:        ")
          .append(entry.getKey())
          .append("\n")
          .append("    numeric attributes: ")
          .append(entry.getValue().stream().sorted().collect(Collectors.joining(", ")))
          .append("\n");
    }
    return sb.toString();
  }

  private static Map<String, Set<String>> getAvailableJmxAttributes() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectName> objectNames = server.queryNames(null, null);
    Map<String, Set<String>> availableJmxMetrics = new HashMap<>();
    for (ObjectName objectName : objectNames) {
      String name = objectName.toString();
      try {
        Set<String> attrs = getJmxAttributes(server, objectName);
        if (!attrs.isEmpty()) {
          availableJmxMetrics.put(name, attrs);
        }
      } catch (Exception e) {
        // log exception at trace level since this is expected in several cases, e.g.
        // "java.lang.UnsupportedOperationException: CollectionUsage threshold is not supported"
        // and available jmx metrics are already only logged at debug
        logger.trace(e.getMessage(), e);
        availableJmxMetrics.put(name, Collections.singleton("<error getting attributes: " + e));
      }
    }
    return availableJmxMetrics;
  }

  private static Set<String> getJmxAttributes(MBeanServer server, ObjectName objectName)
      throws Exception {
    MBeanInfo mbeanInfo = server.getMBeanInfo(objectName);
    Set<String> attributeNames = new HashSet<>();
    for (MBeanAttributeInfo attribute : mbeanInfo.getAttributes()) {
      if (attribute.isReadable()) {
        try {
          Object value = server.getAttribute(objectName, attribute.getName());
          attributeNames.addAll(getNumericAttributes(attribute, value));
        } catch (Exception e) {
          // log exception at trace level since this is expected in several cases, e.g.
          // "java.lang.UnsupportedOperationException: CollectionUsage threshold is not supported"
          // and available jmx metrics are already only logged at debug
          logger.trace(e.getMessage(), e);
        }
      }
    }
    return attributeNames;
  }

  private static List<String> getNumericAttributes(MBeanAttributeInfo attribute, Object value) {
    String attributeType = attribute.getType();
    if (NUMERIC_ATTRIBUTE_TYPES.contains(attributeType) && value instanceof Number) {
      return singletonList(attribute.getName());
    }
    if (BOOLEAN_ATTRIBUTE_TYPES.contains(attributeType) && value instanceof Boolean) {
      return singletonList(attribute.getName());
    }
    if (attributeType.equals("java.lang.Object") && value instanceof Number) {
      return singletonList(attribute.getName());
    }
    if (attributeType.equals("java.lang.String") && value instanceof String) {
      try {
        Double.parseDouble((String) value);
        return singletonList(attribute.getName());
      } catch (NumberFormatException e) {
        // this is expected for non-numeric attributes
        return emptyList();
      }
    }
    if (attributeType.equals(CompositeData.class.getName())) {
      Object openType = attribute.getDescriptor().getFieldValue("openType");
      CompositeType compositeType = null;
      if (openType instanceof CompositeType) {
        compositeType = (CompositeType) openType;
      } else if (openType == null && value instanceof CompositeDataSupport) {
        compositeType = ((CompositeDataSupport) value).getCompositeType();
      }
      if (compositeType != null) {
        return getCompositeTypeAttributeNames(attribute, value, compositeType);
      }
    }
    return emptyList();
  }

  private static List<String> getCompositeTypeAttributeNames(
      MBeanAttributeInfo attribute, Object compositeData, CompositeType compositeType) {
    List<String> attributeNames = new ArrayList<>();
    for (String itemName : compositeType.keySet()) {
      OpenType<?> itemType = compositeType.getType(itemName);
      if (itemType == null) {
        continue;
      }
      String className = itemType.getClassName();
      Class<?> clazz;
      try {
        clazz = Class.forName(className);
      } catch (ClassNotFoundException e) {
        logger.warn(e.getMessage(), e);
        continue;
      }
      if (Number.class.isAssignableFrom(clazz)) {
        attributeNames.add(attribute.getName() + '.' + itemName);
      } else if (clazz == String.class && compositeData instanceof CompositeData) {
        Object val = ((CompositeData) compositeData).get(itemName);
        if (val instanceof String) {
          try {
            Double.parseDouble((String) val);
            attributeNames.add(attribute.getName() + '.' + itemName);
          } catch (NumberFormatException e) {
            // this is expected for non-numeric attributes
          }
        }
      }
    }
    return attributeNames;
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
