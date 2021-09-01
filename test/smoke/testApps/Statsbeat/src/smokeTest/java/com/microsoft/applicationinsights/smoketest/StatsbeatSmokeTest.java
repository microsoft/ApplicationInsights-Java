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
        mockedIngestion.waitForItems(getMetricPredicate("Feature"), 1, 70, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
    assertNotNull(data.getProperties().get("rp"));
    assertNotNull(data.getProperties().get("attach"));
    assertNotNull(data.getProperties().get("cikey"));
    assertNotNull(data.getProperties().get("runtimeVersion"));
    assertNotNull(data.getProperties().get("os"));
    assertNotNull(data.getProperties().get("language"));
    assertNotNull(data.getProperties().get("version"));
    assertNotNull(data.getProperties().get("feature"));
    assertNotNull(data.getProperties().get("type"));
    assertEquals(9, data.getProperties().size());

    List<Envelope> instrumentationMetrics =
        mockedIngestion.waitForItems(getMetricPredicate("Instrumentation"), 1, 70, TimeUnit.SECONDS);

    MetricData instrumentationData = (MetricData) ((Data<?>) instrumentationMetrics.get(0).getData()).getBaseData();
    assertNotNull(instrumentationData.getProperties().get("rp"));
    assertNotNull(instrumentationData.getProperties().get("attach"));
    assertNotNull(instrumentationData.getProperties().get("cikey"));
    assertNotNull(instrumentationData.getProperties().get("runtimeVersion"));
    assertNotNull(instrumentationData.getProperties().get("os"));
    assertNotNull(instrumentationData.getProperties().get("language"));
    assertNotNull(instrumentationData.getProperties().get("version"));
    assertNotNull(instrumentationData.getProperties().get("feature"));
    assertNotNull(instrumentationData.getProperties().get("type"));
    assertEquals(9, instrumentationData.getProperties().size());

    List<Envelope> attachMetrics =
        mockedIngestion.waitForItems(getMetricPredicate("Attach"), 1, 70, TimeUnit.SECONDS);

    MetricData attachData = (MetricData) ((Data<?>) attachMetrics.get(0).getData()).getBaseData();
    assertNotNull(attachData.getProperties().get("rp"));
    assertNotNull(attachData.getProperties().get("attach"));
    assertNotNull(attachData.getProperties().get("cikey"));
    assertNotNull(attachData.getProperties().get("runtimeVersion"));
    assertNotNull(attachData.getProperties().get("os"));
    assertNotNull(attachData.getProperties().get("language"));
    assertNotNull(attachData.getProperties().get("version"));
    assertNotNull(attachData.getProperties().get("rpId"));
    assertEquals(8, attachData.getProperties().size());

    List<Envelope> requestSuccessCountMetrics =
        mockedIngestion.waitForItems(
            getMetricPredicate("Request Success Count"), 1, 70, TimeUnit.SECONDS);

    MetricData requestSuccessCountData =
        (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
    assertNotNull(requestSuccessCountData.getProperties().get("rp"));
    assertNotNull(requestSuccessCountData.getProperties().get("attach"));
    assertNotNull(requestSuccessCountData.getProperties().get("cikey"));
    assertNotNull(requestSuccessCountData.getProperties().get("runtimeVersion"));
    assertNotNull(requestSuccessCountData.getProperties().get("os"));
    assertNotNull(requestSuccessCountData.getProperties().get("language"));
    assertNotNull(requestSuccessCountData.getProperties().get("version"));
    assertNotNull(requestSuccessCountData.getProperties().get("endpoint"));
    assertNotNull(requestSuccessCountData.getProperties().get("host"));
    assertEquals(9, requestSuccessCountData.getProperties().size());

    List<Envelope> requestDurationMetrics =
        mockedIngestion.waitForItems(
            getMetricPredicate("Request Duration"), 1, 70, TimeUnit.SECONDS);

    MetricData requestDurationData =
        (MetricData) ((Data<?>) requestDurationMetrics.get(0).getData()).getBaseData();
    assertNotNull(requestDurationData.getProperties().get("rp"));
    assertNotNull(requestDurationData.getProperties().get("attach"));
    assertNotNull(requestDurationData.getProperties().get("cikey"));
    assertNotNull(requestDurationData.getProperties().get("runtimeVersion"));
    assertNotNull(requestDurationData.getProperties().get("os"));
    assertNotNull(requestDurationData.getProperties().get("language"));
    assertNotNull(requestDurationData.getProperties().get("version"));
    assertNotNull(requestSuccessCountData.getProperties().get("endpoint"));
    assertNotNull(requestSuccessCountData.getProperties().get("host"));
    assertEquals(9, requestDurationData.getProperties().size());
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
