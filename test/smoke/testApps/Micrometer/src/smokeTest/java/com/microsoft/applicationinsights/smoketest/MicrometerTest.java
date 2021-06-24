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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPointType;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

@UseAgent("micrometer")
public class MicrometerTest extends AiSmokeTest {

  @Test
  @TargetUri("/test")
  public void doMostBasicTest() throws Exception {
    mockedIngestion.waitForItems("RequestData", 1);

    // sleep a bit and make sure that the excluded metric is not reported
    Thread.sleep(10000);

    List<Envelope> metricItems =
        mockedIngestion.getItemsEnvelopeDataType("MetricData").stream()
            .filter(
                e -> {
                  MetricData data = (MetricData) ((Data<?>) e.getData()).getBaseData();
                  List<DataPoint> points = data.getMetrics();
                  DataPoint point = points.get(0);
                  return point.getValue() == 1;
                })
            .collect(Collectors.toList());

    MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
    List<DataPoint> points = data.getMetrics();
    assertEquals(1, points.size());

    DataPoint point = points.get(0);

    assertEquals(DataPointType.Measurement, point.getKind());
    assertEquals(1, point.getValue(), 0); // (this was verified above in Predicate also)
    assertEquals("test_counter", point.getName());
    assertNull("getCount was non-null", point.getCount());
    assertNull("getMin was non-null", point.getMin());
    assertNull("getMax was non-null", point.getMax());
    assertNull("getStdDev was non-null", point.getStdDev());
    assertTrue(data.getProperties().isEmpty());
  }
}
