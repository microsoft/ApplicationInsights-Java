// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;

public class MetricAssert extends AbstractAssert<MetricAssert, Envelope> {

  public MetricAssert(Envelope envelope) {
    super(envelope, MetricAssert.class);
  }

  @CanIgnoreReturnValue
  public MetricAssert hasValue(double value) {
    isNotNull();
    assertThat(getDataPoint().getValue()).isEqualTo(value);
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasValueGreaterThanZero() {
    isNotNull();
    assertThat(getDataPoint().getValue()).isGreaterThan(0);
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasCount(int count) {
    isNotNull();
    assertThat(getDataPoint().getCount()).isEqualTo(count);
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasMinGreaterThanZero() {
    isNotNull();
    assertThat(getDataPoint().getMin()).isGreaterThan(0);
    return this;
  }

  @CanIgnoreReturnValue
  public MetricAssert hasMaxGreaterThanZero() {
    isNotNull();
    assertThat(getDataPoint().getMax()).isGreaterThan(0);
    return this;
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricAssert containsPropertiesExactly(Map.Entry<String, String>... entries) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }

    isNotNull();
    MetricData baseData = getMetricData();
    assertThat(baseData.getProperties()).containsExactlyInAnyOrderEntriesOf(map);
    return this;
  }

  @CanIgnoreReturnValue
  public final MetricAssert containsTagKey(String key) {
    isNotNull();
    assertThat(actual.getTags()).containsKey(key);
    return this;
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricAssert containsTags(Map.Entry<String, String>... tags) {
    isNotNull();
    assertThat(actual.getTags()).contains(tags);
    return this;
  }

  @CanIgnoreReturnValue
  public final MetricAssert hasNoInternalAttributes() {
    isNotNull();
    MetricData metricData = getMetricData();
    for (String key : metricData.getProperties().keySet()) {
      assertThat(key).doesNotStartWith("applicationinsights.internal.");
    }
    return this;
  }

  private DataPoint getDataPoint() {
    return getMetricData().getMetrics().get(0);
  }

  private MetricData getMetricData() {
    Data<?> data = (Data<?>) actual.getData();
    return (MetricData) data.getBaseData();
  }
}
