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
import static org.junit.Assert.assertNotNull;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

@UseAgent
public class OpenTelemetryMetricTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/trackDoubleCounterMetric")
  public void trackDoubleCounterMetric() throws Exception {
    validateCounterMetric("trackDoubleCounterMetric");
  }

  @Test
  @TargetUri("/trackLongCounterMetric")
  public void trackLongCounterMetric() throws Exception {
    validateCounterMetric("trackLongCounterMetric");
  }

  @Test
  @TargetUri("/trackDoubleGaugeMetric")
  public void trackDoubleGaugeMetric() throws Exception {
    validateGaugeMetric("trackDoubleGaugeMetric");
  }

  @Test
  @TargetUri("/trackLongGaugeMetric")
  public void trackLongGaugeMetric() throws Exception {
    validateGaugeMetric("trackLongGaugeMetric");
  }

  @Test
  @TargetUri("/trackDoubleHistogramMetric")
  public void trackDoubleHistogramMetric() throws Exception {
    validateHistogramMetric("trackDoubleHistogramMetric");
  }

  @Test
  @TargetUri("/trackLongHistogramMetric")
  public void trackLongHistogramMetric() throws Exception {
    validateHistogramMetric("trackLongHistogramMetric");
  }

  private void validateHistogramMetric(String name) throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate(name), 1, 40, TimeUnit.SECONDS);
    assertEquals(1, metrics.size());

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertEquals("GET /OpenTelemetryMetric/" + name, rd.getName());

    Envelope envelope = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope.getTags();
    assertNotNull(tags.get("ai.internal.sdkVersion"));
    assertEquals(tags.get("ai.cloud.roleInstance"), "testroleinstance");
    assertEquals(tags.get("ai.cloud.role"), "testrolename");
    assertEquals(tags.get("ai.application.ver"), "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertEquals(1, dataPointList.size());
    DataPoint dp = dataPointList.get(0);
    assertEquals(456, dp.getValue(), 0);
    assertEquals(name, dp.getName());
    assertEquals(1, dp.getCount(), 0);
    assertEquals(456, dp.getMin(), 0);
    assertEquals(456, dp.getMax(), 0);

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertEquals(properties.get("tag1"), "abc");
    assertEquals(properties.get("tag2"), "def");
  }

  private void validateGaugeMetric(String name) throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate(name), 1, 40, TimeUnit.SECONDS);
    assertEquals(1, metrics.size());

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertEquals("GET /OpenTelemetryMetric/" + name, rd.getName());

    Envelope envelope = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope.getTags();
    assertNotNull(tags.get("ai.internal.sdkVersion"));
    assertEquals(tags.get("ai.cloud.roleInstance"), "testroleinstance");
    assertEquals(tags.get("ai.cloud.role"), "testrolename");
    assertEquals(tags.get("ai.application.ver"), "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertEquals(1, dataPointList.size());
    DataPoint dp = dataPointList.get(0);
    assertEquals(10, dp.getValue(), 0);
    assertEquals(name, dp.getName());

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertEquals(properties.get("tag1"), "abc");
    assertEquals(properties.get("tag2"), "def");
    assertEquals(properties.get("thing1"), "thing2");
  }

  private void validateCounterMetric(String name) throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate(name), 3, 40, TimeUnit.SECONDS);
    assertEquals(3, metrics.size());

    metrics.sort(
        Comparator.comparing(
            obj -> {
              MetricData metricData = (MetricData) ((Data<?>) obj.getData()).getBaseData();
              List<DataPoint> dataPointList = metricData.getMetrics();
              DataPoint dataPoint = dataPointList.get(0);
              return dataPoint.getValue();
            }));

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertEquals("GET /OpenTelemetryMetric/" + name, rd.getName());

    // validate 1st metric
    Envelope envelope1 = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope1.getTags();
    assertNotNull(tags.get("ai.internal.sdkVersion"));
    assertEquals(tags.get("ai.cloud.roleInstance"), "testroleinstance");
    assertEquals(tags.get("ai.cloud.role"), "testrolename");
    assertEquals(tags.get("ai.application.ver"), "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertEquals(1, dataPointList.size());
    DataPoint dp = dataPointList.get(0);
    assertEquals(2.0, dp.getValue(), 0);
    assertEquals(name, dp.getName());

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertEquals(properties.get("tag1"), "abc");
    assertEquals(properties.get("tag2"), "def");
    assertEquals(properties.get("name"), "apple");
    assertEquals(properties.get("color"), "green");

    // validate 2nd metric
    Envelope envelope2 = metrics.get(1);

    // validate tags
    tags = envelope2.getTags();
    assertNotNull(tags.get("ai.internal.sdkVersion"));
    assertEquals(tags.get("ai.cloud.roleInstance"), "testroleinstance");
    assertEquals(tags.get("ai.cloud.role"), "testrolename");
    assertEquals(tags.get("ai.application.ver"), "123");

    // validate base data
    md = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    dataPointList = md.getMetrics();
    assertEquals(1, dataPointList.size());
    dp = dataPointList.get(0);
    assertEquals(6.0, dp.getValue(), 0);
    assertEquals(name, dp.getName());

    // validate custom dimension
    properties = md.getProperties();
    assertEquals(properties.get("tag1"), "abc");
    assertEquals(properties.get("tag2"), "def");
    assertEquals(properties.get("name"), "apple");
    assertEquals(properties.get("color"), "red");

    // validate 3rd metric
    Envelope envelope3 = metrics.get(2);

    // validate tags
    tags = envelope3.getTags();
    assertNotNull(tags.get("ai.internal.sdkVersion"));
    assertEquals(tags.get("ai.cloud.roleInstance"), "testroleinstance");
    assertEquals(tags.get("ai.cloud.role"), "testrolename");
    assertEquals(tags.get("ai.application.ver"), "123");

    // validate base data
    md = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    dataPointList = md.getMetrics();
    assertEquals(1, dataPointList.size());
    dp = dataPointList.get(0);
    assertEquals(7.0, dp.getValue(), 0);
    assertEquals(name, dp.getName());

    // validate custom dimension
    properties = md.getProperties();
    assertEquals(properties.get("tag1"), "abc");
    assertEquals(properties.get("tag2"), "def");
    assertEquals(properties.get("name"), "lemon");
    assertEquals(properties.get("color"), "yellow");
  }
}
