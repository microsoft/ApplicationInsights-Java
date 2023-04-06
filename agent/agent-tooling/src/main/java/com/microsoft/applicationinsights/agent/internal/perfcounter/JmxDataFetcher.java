// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MsgId.CUSTOM_JMX_METRIC_ERROR;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** A utility class that knows how to fetch JMX data. */
public class JmxDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(JmxDataFetcher.class);

  /**
   * Gets an object name and its attributes to fetch and will return the data.
   *
   * @param objectName The object name to search.
   * @param attributes The attributes that 'belong' to the object name.
   * @return A map that represent each attribute: the key is the displayed name for that attribute
   *     and the value is a list of values found
   * @throws Exception In case the object name is not found.
   */
  public static Map<String, Collection<Object>> fetch(
      String objectName, Collection<JmxAttributeData> attributes) throws Exception {
    Map<String, Collection<Object>> result = new HashMap<>();

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectName> objects = server.queryNames(new ObjectName(objectName), null);
    if (objects.isEmpty()) {
      String errorMsg = String.format("Cannot find object name '%s'", objectName);
      throw new IllegalArgumentException(errorMsg);
    }

    for (JmxAttributeData attribute : attributes) {
      try {
        List<Object> resultForAttribute = fetch(server, objects, attribute.attribute);
        result.put(attribute.metricName, resultForAttribute);
      } catch (Exception e) {
        try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
          logger.warn(
              "Failed to fetch JMX object '{}' with attribute '{}': ",
              objectName,
              attribute.attribute);
        }
        throw e;
      }
    }

    return result;
  }

  private static List<Object> fetch(
      MBeanServer server, Set<ObjectName> objects, String attributeName)
      throws AttributeNotFoundException,
          MBeanException,
          ReflectionException,
          InstanceNotFoundException {
    ArrayList<Object> result = new ArrayList<>();

    List<String> inners = splitByDot(attributeName);

    for (ObjectName object : objects) {
      Object value = server.getAttribute(object, inners.get(0));
      if (inners.size() > 1) {
        if (value != null) {
          // TODO (trask) will support more nesting after moving to upstream otel jmx component
          value = ((CompositeData) value).get(inners.get(1));
        }
      }
      if (value != null) {
        result.add(value);
      }
    }

    return result;
  }

  // This code is copied in from upstream otel java instrumentation repository
  // until we move to upstream version
  private static List<String> splitByDot(String rawName) {
    List<String> components = new ArrayList<>();
    try {
      StringBuilder currentSegment = new StringBuilder();
      boolean escaped = false;
      for (int i = 0; i < rawName.length(); ++i) {
        char ch = rawName.charAt(i);
        if (escaped) {
          // Allow only '\' and '.' to be escaped
          if (ch != '\\' && ch != '.') {
            throw new IllegalArgumentException(
                "Invalid escape sequence in attribute name '" + rawName + "'");
          }
          currentSegment.append(ch);
          escaped = false;
        } else {
          if (ch == '\\') {
            escaped = true;
          } else if (ch == '.') {
            // this is a segment separator
            verifyAndAddNameSegment(components, currentSegment);
            currentSegment = new StringBuilder();
          } else {
            currentSegment.append(ch);
          }
        }
      }

      // The returned list is never empty ...
      verifyAndAddNameSegment(components, currentSegment);

    } catch (IllegalArgumentException unused) {
      // Drop the original exception. We have more meaningful context here.
      throw new IllegalArgumentException("Invalid attribute name '" + rawName + "'");
    }

    return components;
  }

  private static void verifyAndAddNameSegment(List<String> segments, StringBuilder candidate) {
    String newSegment = candidate.toString().trim();
    if (newSegment.isEmpty()) {
      throw new IllegalArgumentException();
    }
    segments.add(newSegment);
  }

  private JmxDataFetcher() {}
}
