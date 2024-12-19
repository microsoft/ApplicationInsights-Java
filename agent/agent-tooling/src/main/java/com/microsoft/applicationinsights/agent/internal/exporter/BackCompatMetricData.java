// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class BackCompatMetricData implements MetricData {

  private static final Pattern NAME_AND_TAG_KEY_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");

  private final MetricData delegate;
  private final String name;

  BackCompatMetricData(MetricData delegate) {
    this.delegate = delegate;
    this.name = backCompatName(delegate.getName());
  }

  private static String backCompatName(String name) {
    return NAME_AND_TAG_KEY_PATTERN.matcher(toSnakeCase(name)).replaceAll("_");
  }

  private static String toSnakeCase(String value) {
    // same logic as micrometer's NamingConvention.snakeCase
    return Arrays.stream(value.split("\\."))
        .filter(Objects::nonNull)
        .collect(Collectors.joining("_"));
  }

  @Override
  public Resource getResource() {
    return delegate.getResource();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return delegate.getInstrumentationScopeInfo();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return delegate.getDescription();
  }

  @Override
  public String getUnit() {
    return delegate.getUnit();
  }

  @Override
  public MetricDataType getType() {
    return delegate.getType();
  }

  @Override
  public Data<?> getData() {
    return delegate.getData();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }
}
