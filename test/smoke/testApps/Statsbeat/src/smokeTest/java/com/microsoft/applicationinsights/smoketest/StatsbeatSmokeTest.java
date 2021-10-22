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
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;

@UseAgent("faststatsbeat")
public class StatsbeatSmokeTest extends AiSmokeTest {

  @Test
  @TargetUri(value = "/index.jsp")
  public void testStatsbeat() throws Exception {
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate("Feature"), 2, 70, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
    assertCommon(data);
    assertNotNull(data.getProperties().get("feature"));
    assertNotNull(data.getProperties().get("type"));
    assertEquals("0", data.getProperties().get("type"));
    assertEquals(9, data.getProperties().size());

    MetricData instrumentationData = (MetricData) ((Data<?>) metrics.get(1).getData()).getBaseData();
    assertCommon(instrumentationData);
    assertNotNull(instrumentationData.getProperties().get("feature"));
    assertNotNull(instrumentationData.getProperties().get("type"));
    assertEquals("1", instrumentationData.getProperties().get("type"));
    assertEquals(9, instrumentationData.getProperties().size());

    List<Envelope> attachMetrics =
        mockedIngestion.waitForItems(getMetricPredicate("Attach"), 1, 70, TimeUnit.SECONDS);

    MetricData attachData = (MetricData) ((Data<?>) attachMetrics.get(0).getData()).getBaseData();
    assertCommon(attachData);
    assertNotNull(attachData.getProperties().get("rpId"));
    assertEquals(8, attachData.getProperties().size());

    List<Envelope> requestSuccessCountMetrics =
        mockedIngestion.waitForItems(
            getMetricPredicate("Request Success Count"), 1, 70, TimeUnit.SECONDS);

    MetricData requestSuccessCountData =
        (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
    assertCommon(requestSuccessCountData);
    assertNotNull(requestSuccessCountData.getProperties().get("endpoint"));
    assertNotNull(requestSuccessCountData.getProperties().get("host"));
    assertEquals(9, requestSuccessCountData.getProperties().size());

    List<Envelope> requestDurationMetrics =
        mockedIngestion.waitForItems(
            getMetricPredicate("Request Duration"), 1, 70, TimeUnit.SECONDS);

    MetricData requestDurationData =
        (MetricData) ((Data<?>) requestDurationMetrics.get(0).getData()).getBaseData();
    assertCommon(requestDurationData);
    assertNotNull(requestSuccessCountData.getProperties().get("endpoint"));
    assertNotNull(requestSuccessCountData.getProperties().get("host"));
    assertEquals(9, requestDurationData.getProperties().size());

    List<Envelope> readFailureCountMetrics =
        mockedIngestion.waitForItems(getMetricPredicate("Read Failure Count"), 1, 70, TimeUnit.SECONDS);
    MetricData readFailureCountData =
        (MetricData) ((Data<?>) readFailureCountMetrics.get(0).getData()).getBaseData();
    assertCommon(readFailureCountData);
    assertEquals(7, readFailureCountData.getProperties().size());

    List<Envelope> writeFailureCountMetrics =
        mockedIngestion.waitForItems(getMetricPredicate("Write Failure Count"), 1, 70, TimeUnit.SECONDS);
    MetricData writeFailureCountData =
        (MetricData) ((Data<?>) writeFailureCountMetrics.get(0).getData()).getBaseData();
    assertCommon(writeFailureCountData);
    assertEquals(7, writeFailureCountData.getProperties().size());
  }

  private void assertCommon(MetricData metricData) {
    assertNotNull(metricData.getProperties().get("rp"));
    assertNotNull(metricData.getProperties().get("attach"));
    assertNotNull(metricData.getProperties().get("cikey"));
    assertNotNull(metricData.getProperties().get("runtimeVersion"));
    assertNotNull(metricData.getProperties().get("os"));
    assertNotNull(metricData.getProperties().get("language"));
    assertNotNull(metricData.getProperties().get("version"));
  }

  private static Predicate<Envelope> getMetricPredicate(String name) {
    return input -> {
      if (!input.getData().getBaseType().equals("MetricData")) {
        return false;
      }
      MetricData md = getBaseData(input);
      return name.equals(md.getMetrics().get(0).getName());
    };
  }
}
