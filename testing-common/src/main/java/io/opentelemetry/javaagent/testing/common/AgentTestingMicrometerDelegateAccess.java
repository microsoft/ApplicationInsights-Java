/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.testing.common;

import com.google.auto.value.AutoValue;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AgentTestingMicrometerDelegateAccess {

  private static final MethodHandle getMeasurements;
  private static final MethodHandle reset;

  static {
    try {
      Class<?> agentTestingExporterFactoryClass =
          AgentClassLoaderAccess.loadClass(
              "io.opentelemetry.javaagent.testing.exporter.AgentTestingMicrometerDelegate");
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      reset =
          lookup.findStatic(
              agentTestingExporterFactoryClass, "reset", MethodType.methodType(void.class));
      getMeasurements =
          lookup.findStatic(
              agentTestingExporterFactoryClass,
              "getMeasurements",
              MethodType.methodType(Object[][].class));
    } catch (Exception e) {
      throw new AssertionError("Error accessing fields with reflection.", e);
    }
  }

  public static void reset() {
    try {
      reset.invokeExact();
    } catch (Throwable t) {
      throw new AssertionError("Could not invoke reset", t);
    }
  }

  public static List<Measurement> getMeasurements() {
    Object[][] data;
    try {
      data = (Object[][]) getMeasurements.invokeExact();
    } catch (Throwable t) {
      throw new AssertionError("Could not invoke getMeasurements", t);
    }
    List<Measurement> measurements = new ArrayList<>();
    for (Object[] d : data) {
      String name = (String) d[0];
      Double value = (Double) d[1];
      Integer count = (Integer) d[2];
      Double min = (Double) d[3];
      Double max = (Double) d[4];
      @SuppressWarnings("unchecked")
      Map<String, String> properties = (Map<String, String>) d[5];
      measurements.add(Measurement.create(name, value, count, min, max, properties));
    }
    return measurements;
  }

  @AutoValue
  public abstract static class Measurement {

    private static Measurement create(
        String name,
        double value,
        Integer count,
        Double min,
        Double max,
        Map<String, String> properties) {
      return new AutoValue_AgentTestingMicrometerDelegateAccess_Measurement(
          name, value, count, min, max, properties);
    }

    public abstract String name();

    public abstract double value();

    @Nullable
    public abstract Integer count();

    @Nullable
    public abstract Double min();

    @Nullable
    public abstract Double max();

    public abstract Map<String, String> properties();
  }
}
