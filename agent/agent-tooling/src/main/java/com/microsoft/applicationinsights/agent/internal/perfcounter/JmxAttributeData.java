// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

/**
 * Represents JMX data of an Attribute The display name The name of the attribute The type of the
 * attribute.
 */
public final class JmxAttributeData {
  public final String metricName;
  public final String attribute;

  public JmxAttributeData(String metricName, String attribute) {
    this.attribute = attribute;
    this.metricName = metricName;
  }
}
