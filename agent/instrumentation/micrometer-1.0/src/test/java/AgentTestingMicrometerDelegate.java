// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

public class AgentTestingMicrometerDelegate implements MicrometerUtil.MicrometerUtilDelegate {

  private final List<Measurement> measurements = new CopyOnWriteArrayList<>();

  @Override
  public void trackMetric(
      String name,
      @Nullable String namespace,
      double value,
      Integer count,
      Double min,
      Double max,
      Map<String, String> properties) {
    measurements.add(new Measurement(name, namespace, value, count, min, max, properties));
  }

  public void reset() {
    measurements.clear();
  }

  public List<Measurement> getMeasurements() {
    return measurements;
  }

  public static class Measurement {

    public final String name;

    @Nullable public final String namespace;
    public final double value;
    public final Integer count;
    public final Double min;
    public final Double max;
    public final Map<String, String> properties;

    private Measurement(
        String name,
        @Nullable String namespace,
        double value,
        Integer count,
        Double min,
        Double max,
        Map<String, String> properties) {
      this.name = name;
      this.namespace = namespace;
      this.value = value;
      this.count = count;
      this.min = min;
      this.max = max;
      this.properties = properties;
    }
  }
}
