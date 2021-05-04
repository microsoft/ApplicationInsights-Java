/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.testing.exporter;

import io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AgentTestingMicrometerDelegate implements MicrometerUtil.MicrometerUtilDelegate {

  public static final AgentTestingMicrometerDelegate instance =
      new AgentTestingMicrometerDelegate();

  private final List<Measurement> measurements = new CopyOnWriteArrayList<>();

  @Override
  public void trackMetric(
      String name,
      double value,
      Integer count,
      Double min,
      Double max,
      Map<String, String> properties) {
    measurements.add(new Measurement(name, value, count, min, max, properties));
  }

  public static void reset() {
    instance.measurements.clear();
  }

  public static Object[][] getMeasurements() {
    List<Measurement> measurements = instance.measurements;
    Object[][] data = new Object[measurements.size()][];
    for (int i = 0; i < data.length; i++) {
      Measurement measurement = measurements.get(i);
      data[i] =
          new Object[] {
            measurement.name,
            measurement.value,
            measurement.count,
            measurement.min,
            measurement.max,
            measurement.properties
          };
    }
    return data;
  }

  private static class Measurement {

    private final String name;
    private final double value;
    private final Integer count;
    private final Double min;
    private final Double max;
    private final Map<String, String> properties;

    private Measurement(
        String name,
        double value,
        Integer count,
        Double min,
        Double max,
        Map<String, String> properties) {
      this.name = name;
      this.value = value;
      this.count = count;
      this.min = min;
      this.max = max;
      this.properties = properties;
    }
  }
}
