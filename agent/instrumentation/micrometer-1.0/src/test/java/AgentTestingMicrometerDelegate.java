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

import com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AgentTestingMicrometerDelegate implements MicrometerUtil.MicrometerUtilDelegate {

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

  public void reset() {
    measurements.clear();
  }

  public List<Measurement> getMeasurements() {
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
