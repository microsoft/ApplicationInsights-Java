package io.opentelemetry.javaagent.testing.common;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
      throw new Error("Error accessing fields with reflection.", e);
    }
  }

  public static void reset() {
    try {
      reset.invokeExact();
    } catch (Throwable t) {
      throw new Error("Could not invoke reset", t);
    }
  }

  public static List<Measurement> getMeasurements() {
    Object[][] data;
    try {
      data = (Object[][]) getMeasurements.invokeExact();
    } catch (Throwable t) {
      throw new Error("Could not invoke getMeasurements", t);
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
      measurements.add(new Measurement(name, value, count, min, max, properties));
    }
    return measurements;
  }

  public static class Measurement {

    public final String name;
    public final double value;
    public final Integer count;
    public final Double min;
    public final Double max;
    public final Map<String, String> properties;

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
