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

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UseAgent("open_telemetry_metric")
public class OpenTelemetryMetricTest extends AiSmokeTest {

  @Test
  @TargetUri("/trackDoubleCounterMetric")
  public void trackDoubleCounterMetric() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate("trackDoubleCounterMetric"), 3, 40, TimeUnit.SECONDS);
    assertEquals(3, metrics.size());

    for (Envelope envelop : metrics) {
      MetricData md = (MetricData) ((Data<?>) envelop.getData()).getBaseData();
      List<DataPoint> dataPointList = md.getMetrics();
      assertEquals(1, dataPointList.size());
      System.out.println("datapoint: " + dataPointList.get(0).getValue());
    }
  }

  @Test
  @TargetUri("/trackLongCounterMetric")
  public void trackLongCounterMetric() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        mockedIngestion
            .waitForItems(getMetricPredicate("trackLongCounterMetric"), 3, 40, TimeUnit.SECONDS);
    assertEquals(3, metrics.size());

    for (Envelope envelop : metrics) {
      MetricData md = (MetricData) ((Data<?>) envelop.getData()).getBaseData();
      List<DataPoint> dataPointList = md.getMetrics();
      assertEquals(1, dataPointList.size());
      System.out.println("datapoint: " + dataPointList.get(0).getValue());
    }
  }
}
